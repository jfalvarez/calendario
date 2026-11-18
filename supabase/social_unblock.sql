-- Calendario · desbloquear a un solicitante (BORRADOR, sin aplicar).
-- Requiere social.sql aplicado. Seguro de repetir.
-- Desbloquear solo quita el bloqueo: no restaura vínculos anteriores y la persona puede volver a pedir vínculo.

create or replace function public.list_blocked()
returns table (
  blocked_id    uuid,
  blocked_alias text,
  blocked_at    timestamptz
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
    select b.blocked_id, pr.alias, b.created_at
      from public.link_blocks b
      left join public.profiles pr on pr.owner_id = b.blocked_id
     where b.owner_id = (select auth.uid())
     order by b.created_at desc;
end;
$$;

create or replace function public.unblock_requester(p_blocked uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (select auth.uid()) is null then
    raise exception 'not_authenticated';
  end if;

  delete from public.link_blocks
   where owner_id = (select auth.uid()) and blocked_id = p_blocked;

  -- Quitar también el enfriamiento de 7 días que dejó el bloqueo: así puede volver a pedir vínculo ya.
  update public.links
     set ended_by = null, updated_at = now()
   where requester_id = p_blocked and owner_id = (select auth.uid())
     and status in ('revoked', 'rejected');
end;
$$;

revoke all on function public.list_blocked()                 from public, anon;
revoke all on function public.unblock_requester(uuid)        from public, anon;
grant execute on function public.list_blocked()              to authenticated;
grant execute on function public.unblock_requester(uuid)     to authenticated;
