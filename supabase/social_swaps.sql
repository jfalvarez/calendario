-- Calendario · intercambio de turnos entre compañeros (BORRADOR, sin aplicar).
-- Requiere haber ejecutado antes supabase/social.sql. Revisar antes de ejecutar. Seguro de repetir.
-- Diseño: docs/social-diseno.md (sección "Intercambio de turnos").
--
-- Reglas:
--  * Propone quien tiene un vínculo aceptado hacia el otro (así puede ver su calendario para elegir el día).
--  * Acepta solo el destinatario. Ninguno de los dos puede escribir en el calendario del otro: el cambio lo hace
--    respond_swap (SECURITY DEFINER) en una sola transacción.
--  * Cada parte entrega un día y recibe el de la otra, en el mismo turno (slot). Ambos días deben estar libres
--    en el lado que recibe, y el turno (shift_id) debe existir en la cuenta que lo recibe.
--  * Los ids de asignación siguen la convención de la app: "AAAA-MM-DD#slot", así que el upsert no choca con
--    filas borradas lógicamente.
--  * updated_at se pone en milisegundos del servidor para que el last-write-wins de la app lo acepte.

create table if not exists public.shift_swaps (
  id              uuid primary key default gen_random_uuid(),
  proposer_id     uuid not null references auth.users (id) on delete cascade,
  target_id       uuid not null references auth.users (id) on delete cascade,
  proposer_date   date not null,
  proposer_slot   smallint not null check (proposer_slot in (0, 1)),
  target_date     date not null,
  target_slot     smallint not null check (target_slot in (0, 1)),
  status          text not null default 'pending'
                  check (status in ('pending', 'accepted', 'rejected', 'cancelled')),
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  check (proposer_id <> target_id)
);

alter table public.shift_swaps enable row level security;
-- Sin políticas: todo pasa por las funciones de abajo.

-- Máximo de propuestas pendientes por persona, para evitar spam.
-- ---------------------------------------------------------------------------
-- Proponer un cambio. Requiere vínculo aceptado proposer → target y que ambos turnos existan.
-- ---------------------------------------------------------------------------
create or replace function public.propose_swap(
  p_target uuid, p_proposer_date date, p_proposer_slot smallint, p_target_date date, p_target_slot smallint
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := (select auth.uid());
  new_id uuid;
begin
  if me is null then raise exception 'not_authenticated'; end if;
  if me = p_target then raise exception 'self_swap'; end if;

  if not exists (
    select 1 from public.links l
     where l.requester_id = me and l.owner_id = p_target and l.status = 'accepted'
  ) then
    raise exception 'forbidden';
  end if;

  if (select count(*) from public.shift_swaps s where s.proposer_id = me and s.status = 'pending') >= 10 then
    raise exception 'too_many_pending';
  end if;

  if not exists (select 1 from public.shift_assignments a
                  where a.owner_id = me and a.assignment_date = p_proposer_date
                    and a.slot = p_proposer_slot and a.deleted_at is null) then
    raise exception 'assignment_missing';
  end if;
  if not exists (select 1 from public.shift_assignments a
                  where a.owner_id = p_target and a.assignment_date = p_target_date
                    and a.slot = p_target_slot and a.deleted_at is null) then
    raise exception 'assignment_missing';
  end if;

  insert into public.shift_swaps (proposer_id, target_id, proposer_date, proposer_slot, target_date, target_slot)
  values (me, p_target, p_proposer_date, p_proposer_slot, p_target_date, p_target_slot)
  returning id into new_id;

  return new_id;
end;
$$;

-- ---------------------------------------------------------------------------
-- Responder a una propuesta (solo el destinatario). Al aceptar, hace el cambio de los dos calendarios.
-- Devuelve 'accepted' o 'rejected'. Errores: swap_not_found, invalid_transition, forbidden, link_revoked,
-- assignment_changed, target_busy, proposer_busy, shift_missing.
-- ---------------------------------------------------------------------------
create or replace function public.respond_swap(p_swap uuid, p_accept boolean)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := (select auth.uid());
  s  public.shift_swaps%rowtype;
  now_ms bigint := (extract(epoch from now()) * 1000)::bigint;
  a  public.shift_assignments%rowtype;  -- lo que entrega el proponente
  b  public.shift_assignments%rowtype;  -- lo que entrega el destinatario
begin
  if me is null then raise exception 'not_authenticated'; end if;

  select * into s from public.shift_swaps where id = p_swap for update;
  if not found then raise exception 'swap_not_found'; end if;
  if me <> s.target_id then raise exception 'forbidden'; end if;
  if s.status <> 'pending' then raise exception 'invalid_transition'; end if;

  if not p_accept then
    update public.shift_swaps set status = 'rejected', updated_at = now() where id = p_swap;
    return 'rejected';
  end if;

  -- El vínculo que permitió proponer debe seguir activo.
  if not exists (select 1 from public.links l
                  where l.requester_id = s.proposer_id and l.owner_id = s.target_id and l.status = 'accepted') then
    raise exception 'link_revoked';
  end if;

  select * into a from public.shift_assignments
   where owner_id = s.proposer_id and assignment_date = s.proposer_date and slot = s.proposer_slot and deleted_at is null
   for update;
  select * into b from public.shift_assignments
   where owner_id = s.target_id and assignment_date = s.target_date and slot = s.target_slot and deleted_at is null
   for update;
  if a.id is null or b.id is null then raise exception 'assignment_changed'; end if;

  -- Cada parte debe tener en su cuenta el turno que recibe.
  if not exists (select 1 from public.shifts sh
                  where sh.owner_id = s.target_id and sh.id = a.shift_id and sh.deleted_at is null) then
    raise exception 'shift_missing';
  end if;
  if not exists (select 1 from public.shifts sh
                  where sh.owner_id = s.proposer_id and sh.id = b.shift_id and sh.deleted_at is null) then
    raise exception 'shift_missing';
  end if;

  -- Cada parte debe tener libre el día que recibe.
  if exists (select 1 from public.shift_assignments x
              where x.owner_id = s.target_id and x.assignment_date = s.proposer_date
                and x.slot = s.proposer_slot and x.deleted_at is null) then
    raise exception 'target_busy';
  end if;
  if exists (select 1 from public.shift_assignments x
              where x.owner_id = s.proposer_id and x.assignment_date = s.target_date
                and x.slot = s.target_slot and x.deleted_at is null) then
    raise exception 'proposer_busy';
  end if;

  -- Quitar los dos turnos que se entregan.
  update public.shift_assignments
     set deleted_at = now_ms, updated_at = now_ms
   where owner_id = s.proposer_id and id = a.id;
  update public.shift_assignments
     set deleted_at = now_ms, updated_at = now_ms
   where owner_id = s.target_id and id = b.id;

  -- Poner cada turno en el calendario que lo recibe. El id es "AAAA-MM-DD#slot", como en la app.
  insert into public.shift_assignments (owner_id, id, assignment_date, slot, shift_id, updated_at, deleted_at)
  values
    (s.target_id,   s.proposer_date::text || '#' || s.proposer_slot::text, s.proposer_date, s.proposer_slot, a.shift_id, now_ms, null),
    (s.proposer_id, s.target_date::text   || '#' || s.target_slot::text,   s.target_date,   s.target_slot,   b.shift_id, now_ms, null)
  on conflict (owner_id, id) do update
    set assignment_date = excluded.assignment_date,
        slot = excluded.slot,
        shift_id = excluded.shift_id,
        updated_at = excluded.updated_at,
        deleted_at = null;

  update public.shift_swaps set status = 'accepted', updated_at = now() where id = p_swap;
  return 'accepted';
end;
$$;

-- ---------------------------------------------------------------------------
-- Cancelar mi propuesta pendiente (solo el proponente).
-- ---------------------------------------------------------------------------
create or replace function public.cancel_swap(p_swap uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (select auth.uid()) is null then raise exception 'not_authenticated'; end if;
  update public.shift_swaps
     set status = 'cancelled', updated_at = now()
   where id = p_swap and proposer_id = (select auth.uid()) and status = 'pending';
  if not found then raise exception 'invalid_transition'; end if;
end;
$$;

-- ---------------------------------------------------------------------------
-- Mis propuestas (entrantes y salientes) con el alias del otro.
-- ---------------------------------------------------------------------------
create or replace function public.list_my_swaps()
returns table (
  swap_id         uuid,
  direction       text,
  other_alias     text,
  proposer_date   date,
  proposer_slot   smallint,
  target_date     date,
  target_slot     smallint,
  status          text
)
language plpgsql
security definer
set search_path = ''
as $$
declare
  me uuid := (select auth.uid());
begin
  if me is null then raise exception 'not_authenticated'; end if;
  return query
    select s.id,
           (case when s.proposer_id = me then 'outgoing' else 'incoming' end)::text,
           pr.alias,
           s.proposer_date, s.proposer_slot, s.target_date, s.target_slot, s.status
      from public.shift_swaps s
      join public.profiles pr
        on pr.owner_id = (case when s.proposer_id = me then s.target_id else s.proposer_id end)
     where (s.proposer_id = me or s.target_id = me)
     order by s.created_at desc;
end;
$$;

revoke all on function public.propose_swap(uuid, date, smallint, date, smallint) from public, anon;
revoke all on function public.respond_swap(uuid, boolean)                          from public, anon;
revoke all on function public.cancel_swap(uuid)                                    from public, anon;
revoke all on function public.list_my_swaps()                                      from public, anon;
grant execute on function public.propose_swap(uuid, date, smallint, date, smallint) to authenticated;
grant execute on function public.respond_swap(uuid, boolean)                          to authenticated;
grant execute on function public.cancel_swap(uuid)                                    to authenticated;
grant execute on function public.list_my_swaps()                                      to authenticated;
