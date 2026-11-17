-- Calendario · esquema de Supabase (Postgres).
-- Pégalo entero en Supabase → SQL Editor → New query → Run. Es seguro volver a ejecutarlo.
-- Documentación: Calendario-arquitectura/07-sincronizacion-supabase.md y 08-guia-supabase.md

-- ---------------------------------------------------------------------------
-- Tablas. Cada fila pertenece a un usuario (owner_id) y la clave primaria es (owner_id, id):
-- los turnos por defecto usan el mismo id ('default-morning'…) en todos los usuarios.
-- Tiempos: updated_at / deleted_at son milisegundos UTC puestos por la app (se usan para
-- decidir qué cambio gana); server_updated_at lo pone el servidor y sirve de cursor para descargar.
-- ---------------------------------------------------------------------------
create table if not exists public.shifts (
  owner_id           uuid    not null default auth.uid() references auth.users (id) on delete cascade,
  id                 text    not null,
  name               text    not null,
  abbreviation       text    not null,
  color_argb         bigint  not null,
  sort_order         integer not null,
  start_minutes      integer,
  end_minutes        integer,
  break_minutes      integer not null default 0,
  updated_at         bigint  not null,
  deleted_at         bigint,
  server_updated_at  timestamptz not null default now(),
  primary key (owner_id, id)
);

create table if not exists public.shift_assignments (
  owner_id           uuid     not null default auth.uid() references auth.users (id) on delete cascade,
  id                 text     not null,
  assignment_date    date     not null,
  slot               smallint not null check (slot in (0, 1)),
  shift_id           text     not null,
  updated_at         bigint   not null,
  deleted_at         bigint,
  server_updated_at  timestamptz not null default now(),
  primary key (owner_id, id),
  unique (owner_id, assignment_date, slot)
);

create index if not exists shifts_cursor_idx on public.shifts (owner_id, server_updated_at);
create index if not exists shift_assignments_cursor_idx on public.shift_assignments (owner_id, server_updated_at);

-- ---------------------------------------------------------------------------
-- Último cambio gana (por registro): si llega una versión más antigua que la guardada se ignora.
-- Si no, se sella server_updated_at para que los demás dispositivos la descarguen.
-- ---------------------------------------------------------------------------
create or replace function public.apply_last_write_wins()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if tg_op = 'UPDATE' and new.updated_at < old.updated_at then
    return old; -- versión obsoleta: se conserva la que ya había
  end if;
  new.server_updated_at := now();
  return new;
end;
$$;

drop trigger if exists shifts_lww on public.shifts;
create trigger shifts_lww before insert or update on public.shifts
  for each row execute function public.apply_last_write_wins();

drop trigger if exists shift_assignments_lww on public.shift_assignments;
create trigger shift_assignments_lww before insert or update on public.shift_assignments
  for each row execute function public.apply_last_write_wins();

-- ---------------------------------------------------------------------------
-- Seguridad por filas (RLS): cada usuario sólo ve y escribe lo suyo.
-- Sin estas políticas, la clave pública permitiría leer los datos de todos.
-- No hay política de DELETE: la app borra de forma lógica (deleted_at).
-- ---------------------------------------------------------------------------
alter table public.shifts            enable row level security;
alter table public.shift_assignments enable row level security;

drop policy if exists shifts_select on public.shifts;
drop policy if exists shifts_insert on public.shifts;
drop policy if exists shifts_update on public.shifts;
create policy shifts_select on public.shifts for select to authenticated
  using (owner_id = (select auth.uid()));
create policy shifts_insert on public.shifts for insert to authenticated
  with check (owner_id = (select auth.uid()));
create policy shifts_update on public.shifts for update to authenticated
  using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));

drop policy if exists assignments_select on public.shift_assignments;
drop policy if exists assignments_insert on public.shift_assignments;
drop policy if exists assignments_update on public.shift_assignments;
create policy assignments_select on public.shift_assignments for select to authenticated
  using (owner_id = (select auth.uid()));
create policy assignments_insert on public.shift_assignments for insert to authenticated
  with check (owner_id = (select auth.uid()));
create policy assignments_update on public.shift_assignments for update to authenticated
  using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- Festivos locales (marcados por el usuario). Se identifican por (owner_id, holiday_date).
-- Mismo esquema de sincronización que el resto: borrado lógico + último cambio gana.
-- ---------------------------------------------------------------------------
create table if not exists public.local_holidays (
  owner_id           uuid   not null default auth.uid() references auth.users (id) on delete cascade,
  holiday_date       date   not null,
  updated_at         bigint not null,
  deleted_at         bigint,
  server_updated_at  timestamptz not null default now(),
  primary key (owner_id, holiday_date)
);

create index if not exists local_holidays_cursor_idx on public.local_holidays (owner_id, server_updated_at);

drop trigger if exists local_holidays_lww on public.local_holidays;
create trigger local_holidays_lww before insert or update on public.local_holidays
  for each row execute function public.apply_last_write_wins();

alter table public.local_holidays enable row level security;

drop policy if exists holidays_select on public.local_holidays;
drop policy if exists holidays_insert on public.local_holidays;
drop policy if exists holidays_update on public.local_holidays;
create policy holidays_select on public.local_holidays for select to authenticated
  using (owner_id = (select auth.uid()));
create policy holidays_insert on public.local_holidays for insert to authenticated
  with check (owner_id = (select auth.uid()));
create policy holidays_update on public.local_holidays for update to authenticated
  using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- Política de versiones (actualizaciones forzadas). Una fila por plataforma; la app la lee al arrancar.
-- Solo se edita desde el panel de Supabase: no hay políticas de insert/update/delete. No guarda datos de usuario.
-- Ej.: update public.app_config set min_version_code = 13, updated_at = now() where platform = 'android';
-- ---------------------------------------------------------------------------
create table if not exists public.app_config (
  platform                  text primary key check (platform in ('android', 'desktop')),
  min_version_code          integer not null default 1,
  recommended_version_code  integer not null default 1,
  message                   text,
  store_url                 text,
  updated_at                timestamptz not null default now(),
  check (recommended_version_code >= min_version_code)
);

alter table public.app_config enable row level security;

drop policy if exists app_config_select on public.app_config;
create policy app_config_select on public.app_config for select to anon, authenticated using (true);

insert into public.app_config (platform) values ('android'), ('desktop') on conflict do nothing;
