-- Calendario · compartir turnos en solo lectura con alias (BORRADOR, sin aplicar).
-- Revisar antes de ejecutar en Supabase → SQL Editor. Es seguro volver a ejecutarlo.
-- Diseño: docs/social-diseno.md
--
-- Principios:
--  * Nada de las tablas propias (shifts, shift_assignments…) cambia de política: el SyncEngine no ve datos ajenos.
--  * Los datos ajenos sólo salen por funciones SECURITY DEFINER que comprueban un vínculo aceptado.
--  * Las escrituras de vínculos y bloqueos van por funciones, no por políticas INSERT/UPDATE/DELETE.
--  * request_link devuelve un código de texto en vez de lanzar errores: así el intento queda registrado
--    (un RAISE deshace la transacción y borraría el registro del límite de intentos).

-- ---------------------------------------------------------------------------
-- Perfil: alias público elegido por el usuario (lo que se dice «de boca a boca»).
-- Único sin distinguir mayúsculas. El dueño puede cambiarlo; los vínculos siguen por owner_id.
-- ---------------------------------------------------------------------------
create table if not exists public.profiles (
  owner_id    uuid primary key references auth.users (id) on delete cascade,
  alias       text,
  created_at  timestamptz not null default now(),
  check (alias is null or alias ~ '^[A-Za-z0-9_]{3,20}$')
);

create unique index if not exists profiles_alias_lower_idx
  on public.profiles (lower(alias)) where alias is not null;

alter table public.profiles enable row level security;

drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own on public.profiles for select to authenticated
  using (owner_id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- Registro de búsquedas por alias (límite de intentos). Sólo lo escriben las funciones.
-- ---------------------------------------------------------------------------
create table if not exists public.link_attempts (
  user_id       uuid not null references auth.users (id) on delete cascade,
  attempted_at  timestamptz not null default now()
);

create index if not exists link_attempts_idx on public.link_attempts (user_id, attempted_at);

alter table public.link_attempts enable row level security;

-- ---------------------------------------------------------------------------
-- Bloqueos: `owner_id` ha bloqueado a `blocked_id`; sus solicitudes ya no se crean.
-- ---------------------------------------------------------------------------
create table if not exists public.link_blocks (
  owner_id    uuid not null references auth.users (id) on delete cascade,
  blocked_id  uuid not null references auth.users (id) on delete cascade,
  created_at  timestamptz not null default now(),
  primary key (owner_id, blocked_id),
  check (owner_id <> blocked_id)
);

alter table public.link_blocks enable row level security;

-- ---------------------------------------------------------------------------
-- Vínculos. `requester_id` quiere ver los turnos de `owner_id`.
-- status: pending (esperando al dueño), accepted (visible), rejected (el dueño dijo que no),
--         revoked (cortado). `ended_by` dice quién lo cerró: si fue el dueño, hay enfriamiento de 7 días.
-- ---------------------------------------------------------------------------
create table if not exists public.links (
  id            uuid primary key default gen_random_uuid(),
  requester_id  uuid not null references auth.users (id) on delete cascade,
  owner_id      uuid not null references auth.users (id) on delete cascade,
  status        text not null default 'pending' check (status in ('pending', 'accepted', 'rejected', 'revoked')),
  ended_by      uuid references auth.users (id) on delete set null,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  unique (requester_id, owner_id),
  check (requester_id <> owner_id)
);

alter table public.links enable row level security;

drop policy if exists links_select_party on public.links;
create policy links_select_party on public.links for select to authenticated
  using (requester_id = (select auth.uid()) or owner_id = (select auth.uid()));
-- Sin políticas de insert/update/delete: todo pasa por las funciones de abajo.

-- ---------------------------------------------------------------------------
-- Fijar el alias (una sola vez). Lanza alias_locked si ya tiene uno, alias_taken si otro usuario lo tiene
-- (sin distinguir mayúsculas). Un alias fijado no se puede cambiar: es la identidad con la que te encuentran.
-- ---------------------------------------------------------------------------
create or replace function public.set_my_alias(p_alias text)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := (select auth.uid());
  a  text := trim(p_alias);
begin
  if me is null then
    raise exception 'not_authenticated';
  end if;
  if exists (select 1 from public.profiles pr where pr.owner_id = me and pr.alias is not null) then
    raise exception 'alias_locked';
  end if;
  if a is null or a !~ '^[A-Za-z0-9_]{3,20}$' then
    raise exception 'invalid_alias';
  end if;
  if exists (select 1 from public.profiles pr where lower(pr.alias) = lower(a) and pr.owner_id <> me) then
    raise exception 'alias_taken';
  end if;

  insert into public.profiles (owner_id, alias) values (me, a)
  on conflict (owner_id) do update set alias = excluded.alias;

  return a;
exception
  when unique_violation then
    raise exception 'alias_taken';
end;
$$;

-- ---------------------------------------------------------------------------
-- Pedir vínculo por alias. Devuelve un código (no lanza errores de negocio):
--   'sent'                 solicitud creada, o ya existía, o el dueño la tiene bloqueada / rechazada hace poco.
--                          Se responde igual en todos esos casos para no revelar bloqueos ni rechazos.
--   'alias_not_found'      no existe ese alias
--   'self'                 es tu propio alias
--   'too_many_attempts'    más de 20 búsquedas en la última hora
--   'too_many_pending'     ya tienes 5 solicitudes pendientes
-- Búsqueda exacta: no hay forma de listar ni buscar por prefijo.
-- ---------------------------------------------------------------------------
create or replace function public.request_link(p_alias text)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  me     uuid := (select auth.uid());
  target uuid;
  cur    public.links%rowtype;
  found_link boolean;
begin
  if me is null then
    raise exception 'not_authenticated';
  end if;

  -- El intento se registra antes de cualquier comprobación de éxito.
  if (select count(*) from public.link_attempts la
       where la.user_id = me and la.attempted_at > now() - interval '1 hour') >= 20 then
    return 'too_many_attempts';
  end if;
  insert into public.link_attempts (user_id) values (me);

  select pr.owner_id into target
    from public.profiles pr
   where lower(pr.alias) = lower(trim(p_alias));
  if target is null then
    return 'alias_not_found';
  end if;
  if target = me then
    return 'self';
  end if;

  if exists (select 1 from public.link_blocks b where b.owner_id = target and b.blocked_id = me) then
    return 'sent';
  end if;

  select * into cur from public.links l where l.requester_id = me and l.owner_id = target;
  found_link := found;
  if found_link then
    if cur.status in ('pending', 'accepted') then
      return 'sent';
    end if;
    -- Rechazada o revocada por el dueño: esperar 7 días antes de volver a pedirla.
    if cur.ended_by = target and cur.updated_at > now() - interval '7 days' then
      return 'sent';
    end if;
  end if;

  if (select count(*) from public.links l where l.requester_id = me and l.status = 'pending') >= 5 then
    return 'too_many_pending';
  end if;

  if found_link then
    update public.links
       set status = 'pending', ended_by = null, updated_at = now()
     where id = cur.id;
  else
    insert into public.links (requester_id, owner_id, status) values (me, target, 'pending');
  end if;

  return 'sent';
end;
$$;

-- ---------------------------------------------------------------------------
-- Cambiar estado de un vínculo:
--  * El dueño acepta o rechaza una solicitud pendiente, o revoca un vínculo pendiente/aceptado.
--  * El solicitante sólo puede cancelar o dejar de ver (revoked), desde pending o accepted.
-- ---------------------------------------------------------------------------
create or replace function public.set_link_status(p_link uuid, p_status text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := (select auth.uid());
  l  public.links%rowtype;
begin
  if me is null then
    raise exception 'not_authenticated';
  end if;

  select * into l from public.links where id = p_link for update;
  if not found then
    raise exception 'link_not_found';
  end if;

  if me = l.owner_id then
    if not (
         (p_status = 'accepted' and l.status = 'pending')
      or (p_status = 'rejected' and l.status = 'pending')
      or (p_status = 'revoked'  and l.status in ('pending', 'accepted'))
    ) then
      raise exception 'invalid_transition';
    end if;
  elsif me = l.requester_id then
    if not (p_status = 'revoked' and l.status in ('pending', 'accepted')) then
      raise exception 'invalid_transition';
    end if;
  else
    raise exception 'forbidden';
  end if;

  update public.links
     set status = p_status,
         ended_by = case when p_status = 'accepted' then null else me end,
         updated_at = now()
   where id = p_link;
end;
$$;

-- ---------------------------------------------------------------------------
-- Bloquear al solicitante: corta el vínculo (si lo hay) y evita nuevas solicitudes suyas.
-- ---------------------------------------------------------------------------
create or replace function public.block_requester(p_link uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := (select auth.uid());
  l  public.links%rowtype;
begin
  if me is null then
    raise exception 'not_authenticated';
  end if;

  select * into l from public.links where id = p_link for update;
  if not found then
    raise exception 'link_not_found';
  end if;
  if me <> l.owner_id then
    raise exception 'forbidden';
  end if;

  insert into public.link_blocks (owner_id, blocked_id) values (me, l.requester_id)
  on conflict do nothing;

  if l.status in ('pending', 'accepted') then
    update public.links
       set status = 'revoked', ended_by = me, updated_at = now()
     where id = p_link;
  end if;
end;
$$;

-- ---------------------------------------------------------------------------
-- Solicitudes pendientes que me han hecho (para el banner de la pantalla principal).
-- ---------------------------------------------------------------------------
create or replace function public.list_pending_requests()
returns table (
  link_id         uuid,
  requester_alias text,
  created_at      timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (select auth.uid()) is null then
    raise exception 'not_authenticated';
  end if;

  return query
    select l.id, pr.alias, l.created_at
      from public.links l
      join public.profiles pr on pr.owner_id = l.requester_id
     where l.owner_id = (select auth.uid())
       and l.status = 'pending'
     order by l.created_at;
end;
$$;

-- ---------------------------------------------------------------------------
-- Mis vínculos en ambas direcciones: incoming = me ven mis turnos; outgoing = yo veo los suyos.
-- ---------------------------------------------------------------------------
create or replace function public.list_my_links()
returns table (
  link_id      uuid,
  direction    text,
  other_id     uuid,
  other_alias  text,
  status       text,
  updated_at   timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := (select auth.uid());
begin
  if me is null then
    raise exception 'not_authenticated';
  end if;

  return query
    select l.id,
           (case when l.owner_id = me then 'incoming' else 'outgoing' end)::text,
           (case when l.owner_id = me then l.requester_id else l.owner_id end),
           pr.alias,
           l.status,
           l.updated_at
      from public.links l
      join public.profiles pr
        on pr.owner_id = (case when l.owner_id = me then l.requester_id else l.owner_id end)
     where l.owner_id = me or l.requester_id = me
     order by l.updated_at desc;
end;
$$;

-- ---------------------------------------------------------------------------
-- Turnos ajenos en un rango de fechas (solo lectura). Requiere vínculo accepted del solicitante con `p_owner`.
-- Sólo expone turnos asignados y su definición; nunca notas, deudas ni festivos.
-- ---------------------------------------------------------------------------
create or replace function public.get_shared_schedule(p_owner uuid, p_from date, p_to date)
returns table (
  assignment_date  date,
  slot             smallint,
  shift_id         text,
  shift_name       text,
  abbreviation     text,
  color_argb       bigint,
  start_minutes    integer,
  end_minutes      integer,
  break_minutes    integer
)
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not exists (
    select 1 from public.links l
     where l.requester_id = (select auth.uid())
       and l.owner_id = p_owner
       and l.status = 'accepted'
  ) then
    raise exception 'forbidden';
  end if;

  return query
    select a.assignment_date, a.slot, a.shift_id,
           s.name, s.abbreviation, s.color_argb, s.start_minutes, s.end_minutes, s.break_minutes
      from public.shift_assignments a
      join public.shifts s
        on s.owner_id = a.owner_id and s.id = a.shift_id and s.deleted_at is null
     where a.owner_id = p_owner
       and a.deleted_at is null
       and a.assignment_date between p_from and p_to
     order by a.assignment_date, a.slot;
end;
$$;

-- ---------------------------------------------------------------------------
-- Permisos de ejecución: sólo usuarios autenticados (no la clave anónima).
-- ---------------------------------------------------------------------------
revoke all on function public.set_my_alias(text)                       from public, anon;
revoke all on function public.request_link(text)                       from public, anon;
revoke all on function public.set_link_status(uuid, text)              from public, anon;
revoke all on function public.block_requester(uuid)                    from public, anon;
revoke all on function public.list_pending_requests()                  from public, anon;
revoke all on function public.list_my_links()                          from public, anon;
revoke all on function public.get_shared_schedule(uuid, date, date)    from public, anon;

grant execute on function public.set_my_alias(text)                       to authenticated;
grant execute on function public.request_link(text)                       to authenticated;
grant execute on function public.set_link_status(uuid, text)              to authenticated;
grant execute on function public.block_requester(uuid)                    to authenticated;
grant execute on function public.list_pending_requests()                  to authenticated;
grant execute on function public.list_my_links()                          to authenticated;
grant execute on function public.get_shared_schedule(uuid, date, date)    to authenticated;
