-- Calendario · avisos push (BORRADOR, sin aplicar).
-- Requiere social.sql y social_swaps.sql ya aplicados. Requiere además la extensión pg_net.
-- Diseño: docs/social-diseno.md (sección "Avisos push").
--
-- Qué hace:
--  * device_tokens: un token FCM por dispositivo. Solo lo escribe register_device_token (función).
--  * Disparadores en links y shift_swaps: al crear o cambiar una solicitud/propuesta, llaman a la Edge Function
--    notify-push con el id del destinatario y un texto genérico. El texto no incluye alias ni turnos
--    (la notificación puede verse en la pantalla de bloqueo).
--  * La función envía a todos los tokens del destinatario. Los tokens que FCM rechaza se borran desde la Edge Function.
--
-- Antes de ejecutar: en Supabase, activar la extensión pg_net y guardar en Vault (o como setting de base de datos)
-- la URL de la Edge Function y un secreto compartido. Ver docs/social-diseno.md.

create extension if not exists pg_net with schema extensions;

create table if not exists public.device_tokens (
  owner_id    uuid not null references auth.users (id) on delete cascade,
  token       text not null check (char_length(token) between 20 and 4096),
  platform    text not null default 'android' check (platform in ('android', 'desktop')),
  updated_at  timestamptz not null default now(),
  primary key (owner_id, token)
);

alter table public.device_tokens enable row level security;
-- Sin políticas: la app solo usa register/unregister_device_token; la Edge Function usa la clave de servicio.

create or replace function public.register_device_token(p_token text, p_platform text default 'android')
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (select auth.uid()) is null then raise exception 'not_authenticated'; end if;
  insert into public.device_tokens (owner_id, token, platform, updated_at)
  values ((select auth.uid()), p_token, p_platform, now())
  on conflict (owner_id, token) do update set updated_at = now(), platform = excluded.platform;
end;
$$;

create or replace function public.unregister_device_token(p_token text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (select auth.uid()) is null then raise exception 'not_authenticated'; end if;
  delete from public.device_tokens where owner_id = (select auth.uid()) and token = p_token;
end;
$$;

revoke all on function public.register_device_token(text, text) from public, anon;
revoke all on function public.unregister_device_token(text)     from public, anon;
grant execute on function public.register_device_token(text, text) to authenticated;
grant execute on function public.unregister_device_token(text)     to authenticated;

-- ---------------------------------------------------------------------------
-- Envío: llama a la Edge Function notify-push. La URL y el secreto se leen de la tabla de configuración.
-- Si no hay configuración, no hace nada (así las pruebas locales no fallan).
-- ---------------------------------------------------------------------------
create table if not exists public.push_config (
  key    text primary key,
  value  text not null
);
alter table public.push_config enable row level security;
-- Sin políticas: solo lo lee el disparador (SECURITY DEFINER). Rellenar desde el SQL Editor:
--   insert into public.push_config (key, value) values ('notify_url', 'https://<proyecto>.supabase.co/functions/v1/notify-push'),
--     ('notify_secret', '<secreto largo>') on conflict (key) do update set value = excluded.value;

create or replace function public.send_push(p_user uuid, p_title text, p_body text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_url text;
  v_secret text;
begin
  select value into v_url from public.push_config where key = 'notify_url';
  select value into v_secret from public.push_config where key = 'notify_secret';
  if v_url is null or v_secret is null then return; end if;

  perform net.http_post(
    url := v_url,
    headers := jsonb_build_object('Content-Type', 'application/json', 'x-notify-secret', v_secret),
    body := jsonb_build_object('user_id', p_user, 'title', p_title, 'body', p_body)
  );
end;
$$;

revoke all on function public.send_push(uuid, text, text) from public, anon, authenticated;

-- Solicitud nueva de vínculo: avisa al dueño.
create or replace function public.notify_link_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' and new.status = 'pending' then
    perform public.send_push(new.owner_id, 'Solicitud de compañero', 'Tienes una solicitud pendiente.');
  elsif tg_op = 'UPDATE' and new.status = 'accepted' and old.status <> 'accepted' then
    perform public.send_push(new.requester_id, 'Solicitud aceptada', 'Ya puedes ver los turnos de tu compañero.');
  end if;
  return new;
end;
$$;

drop trigger if exists links_push on public.links;
create trigger links_push after insert or update on public.links
  for each row execute function public.notify_link_change();

-- Propuesta nueva o respuesta: avisa a la otra parte.
create or replace function public.notify_swap_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' and new.status = 'pending' then
    perform public.send_push(new.target_id, 'Propuesta de cambio', 'Tienes una propuesta de cambio de turno.');
  elsif tg_op = 'UPDATE' and new.status in ('accepted', 'rejected') and old.status = 'pending' then
    perform public.send_push(new.proposer_id,
      case when new.status = 'accepted' then 'Cambio aceptado' else 'Cambio rechazado' end,
      'Revisa tus turnos en la app.');
  end if;
  return new;
end;
$$;

drop trigger if exists shift_swaps_push on public.shift_swaps;
create trigger shift_swaps_push after insert or update on public.shift_swaps
  for each row execute function public.notify_swap_change();
