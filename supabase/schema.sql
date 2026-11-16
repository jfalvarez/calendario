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
