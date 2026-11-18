# Prueba de seguridad (RLS) contra el Supabase REAL del proyecto, con dos cuentas de prueba.
#
# Más simple: sin variables de entorno crea él mismo dos cuentas temporales (ver «Modo automático» abajo).
# Uso con cuentas propias (PowerShell, en la raíz del proyecto). Las credenciales van por variables de entorno de ESTA sesión,
# nada se guarda en disco ni se imprime:
#   $env:RLS_A_EMAIL='a@ejemplo.com'; $env:RLS_A_PASS='...'
#   $env:RLS_B_EMAIL='b@ejemplo.com'; $env:RLS_B_PASS='...'
#   powershell -ExecutionPolicy Bypass -File supabase\rls-test.ps1
# Las dos cuentas deben existir y tener el correo confirmado. Lee la URL y la clave pública de local.properties.
# Deja en la cuenta A algunas filas de prueba (id «rlstest-…») marcadas como borradas (borrado lógico).

$ErrorActionPreference = 'Stop'
$props = Get-Content (Join-Path $PSScriptRoot '..\local.properties') -Raw
$url = ([regex]::Match($props, '(?m)^supabase\.url=(.+)$').Groups[1].Value).Trim().TrimEnd('/')
$anon = ([regex]::Match($props, '(?m)^supabase\.anonKey=(.+)$').Groups[1].Value).Trim()
if (-not $url -or -not $anon) { throw 'Falta supabase.url / supabase.anonKey en local.properties' }
# Modo automático: sin variables de entorno, crea dos cuentas temporales (rlstest-…@example.com) con contraseñas
# aleatorias que sólo viven en memoria. Requiere «Confirm email» DESACTIVADO en Supabase mientras se ejecuta.
$auto = -not ($env:RLS_A_EMAIL -and $env:RLS_A_PASS -and $env:RLS_B_EMAIL -and $env:RLS_B_PASS)

function Call($method, $path, $token, $body = $null, $extra = @{}) {
    $headers = @{ apikey = $anon; Prefer = 'return=representation' }
    foreach ($k in $extra.Keys) { $headers[$k] = $extra[$k] } # sustituye (no suma) la cabecera Prefer
    if ($token) { $headers.Authorization = "Bearer $token" }
    $req = @{ Uri = "$url$path"; Method = $method; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $body) { $req.Body = ($body | ConvertTo-Json -Depth 5 -Compress); $req.ContentType = 'application/json' }
    try {
        $r = Invoke-WebRequest @req
        $status = [int]$r.StatusCode; $text = $r.Content
    } catch {
        $resp = $_.Exception.Response
        if (-not $resp) { throw }
        $status = [int]$resp.StatusCode
        # PowerShell 5.1 ya leyó el cuerpo del error y lo deja en ErrorDetails; releer el stream da vacío.
        $text = if ($_.ErrorDetails -and $_.ErrorDetails.Message) { $_.ErrorDetails.Message }
                else { (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd() }
    }
    $json = $null
    try { $json = $text | ConvertFrom-Json } catch { }
    [pscustomobject]@{ Status = $status; Json = $json; Text = $text }
}

function Login($email, $pass) {
    $r = Call 'POST' '/auth/v1/token?grant_type=password' $null @{ email = $email; password = $pass }
    if ($r.Status -ne 200) { throw "No se pudo iniciar sesión con $email (HTTP $($r.Status)). ¿Existe y está confirmada?" }
    [pscustomobject]@{ Token = $r.Json.access_token; Uid = $r.Json.user.id }
}

$script:fails = 0
function Check($name, [bool]$ok, $detail = '') {
    if ($ok) { Write-Host "PASS  $name" -ForegroundColor Green }
    else { Write-Host "FAIL  $name  $detail" -ForegroundColor Red; $script:fails++ }
}
function Count($r) { if ($r.Json -is [array]) { $r.Json.Count } elseif ($null -eq $r.Json) { 0 } else { 1 } }
function Denied($r) { ($r.Status -in 401, 403) -or ($r.Status -eq 400 -and $r.Text -match '42501|row-level security') }

function SignUp($email) {
    $pass = [guid]::NewGuid().ToString('N') + 'aA1!'
    $r = Call 'POST' '/auth/v1/signup' $null @{ email = $email; password = $pass }
    if ($r.Status -ne 200 -or -not $r.Json.access_token) {
        throw "No se pudo crear $email (HTTP $($r.Status)). ¿Está desactivado «Confirm email»? $($r.Text)"
    }
    [pscustomobject]@{ Token = $r.Json.access_token; Uid = $r.Json.user.id }
}

if ($auto) {
    $tag = [guid]::NewGuid().ToString('N').Substring(0, 8)
    $emailA = "rlstest-a-$tag@example.com"; $emailB = "rlstest-b-$tag@example.com"
    $a = SignUp $emailA
    $b = SignUp $emailB
    Write-Host "Cuentas temporales creadas: $emailA y $emailB"
} else {
    $a = Login $env:RLS_A_EMAIL $env:RLS_A_PASS
    $b = Login $env:RLS_B_EMAIL $env:RLS_B_PASS
}
if ($a.Uid -eq $b.Uid) { throw 'A y B son la misma cuenta' }
$now = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$id = "rlstest-$([guid]::NewGuid().ToString('N').Substring(0,12))"
$note = @{ id = $id; note_date = '2026-01-01'; kind = 'Note'; body = 'rls-test'; updated_at = $now }

Write-Host "`n== Sin sesión (anon) =="
foreach ($t in 'shifts', 'shift_assignments', 'local_holidays', 'day_notes') {
    $r = Call 'GET' "/rest/v1/${t}?select=*" $null
    Check "anon no lee $t" ((Denied $r) -or ((Count $r) -eq 0)) "HTTP $($r.Status), filas=$(Count $r)"
}
$r = Call 'POST' '/rest/v1/day_notes' $null $note
Check 'anon no puede insertar notas' (Denied $r) "HTTP $($r.Status)"

Write-Host "`n== Aislamiento entre usuarios A y B (day_notes) =="
$r = Call 'POST' '/rest/v1/day_notes' $a.Token $note
Check 'A inserta su nota' ($r.Status -eq 201) "HTTP $($r.Status) $($r.Text)"
$r = Call 'GET' "/rest/v1/day_notes?id=eq.$id" $b.Token
Check 'B NO ve la nota de A' ((Count $r) -eq 0) "B ve $(Count $r) filas"
$r = Call 'PATCH' "/rest/v1/day_notes?id=eq.$id" $b.Token @{ body = 'hackeado'; updated_at = $now + 1000 }
Check 'B NO puede modificar la nota de A' ((Count $r) -eq 0) "HTTP $($r.Status), filas=$(Count $r)"
$r = Call 'GET' "/rest/v1/day_notes?id=eq.$id" $a.Token
Check 'La nota de A sigue intacta' ((Count $r) -eq 1 -and $r.Json[0].body -eq 'rls-test') $r.Text
$r = Call 'DELETE' "/rest/v1/day_notes?id=eq.$id" $b.Token
Check 'B NO puede borrar la nota de A' ((Count $r) -eq 0) "HTTP $($r.Status)"
$r = Call 'DELETE' "/rest/v1/day_notes?id=eq.$id" $a.Token
Check 'Ni A puede hacer DELETE real (sólo borrado lógico)' ((Count $r) -eq 0) "HTTP $($r.Status), filas=$(Count $r)"
$r = Call 'GET' "/rest/v1/day_notes?id=eq.$id" $a.Token
Check 'La nota de A sigue existiendo tras los DELETE' ((Count $r) -eq 1)

Write-Host "`n== Suplantación de propietario =="
$spoof = @{ id = "$id-spoof"; owner_id = $a.Uid; note_date = '2026-01-01'; kind = 'Note'; body = 'suplantada'; updated_at = $now }
$r = Call 'POST' '/rest/v1/day_notes' $b.Token $spoof
Check 'B NO puede insertar filas con owner_id de A' (Denied $r) "HTTP $($r.Status) $($r.Text)"
$r = Call 'PATCH' "/rest/v1/day_notes?id=eq.$id" $a.Token @{ owner_id = $b.Uid; updated_at = $now + 2000 }
Check 'A NO puede regalar su fila cambiando owner_id' ((Denied $r) -or ((Count $r) -eq 0)) "HTTP $($r.Status) $($r.Text)"
$upsert = @{ id = $id; owner_id = $a.Uid; note_date = '2026-01-01'; kind = 'Note'; body = 'pisada'; updated_at = $now + 3000 }
$r = Call 'POST' '/rest/v1/day_notes?on_conflict=owner_id,id' $b.Token $upsert @{ Prefer = 'resolution=merge-duplicates,return=representation' }
Check 'B NO puede pisar la fila de A con upsert' (Denied $r) "HTTP $($r.Status) $($r.Text)"

Write-Host "`n== Otras tablas (misma política) =="
$shift = @{ id = "$id-shift"; name = 'rls'; abbreviation = 'R'; color_argb = 1; sort_order = 99; updated_at = $now }
$r = Call 'POST' '/rest/v1/shifts' $a.Token $shift
Check 'A inserta un turno' ($r.Status -eq 201) "HTTP $($r.Status) $($r.Text)"
$r = Call 'GET' "/rest/v1/shifts?id=eq.$id-shift" $b.Token
Check 'B NO ve el turno de A' ((Count $r) -eq 0)
$r = Call 'PATCH' "/rest/v1/shifts?id=eq.$id-shift" $b.Token @{ name = 'hackeado'; updated_at = $now + 1000 }
Check 'B NO puede modificar el turno de A' ((Count $r) -eq 0)

Write-Host "`n== app_config (sólo lectura) =="
$r = Call 'GET' '/rest/v1/app_config?select=platform' $null
Check 'anon puede leer app_config' ($r.Status -eq 200 -and (Count $r) -ge 1) "HTTP $($r.Status)"
$r = Call 'PATCH' '/rest/v1/app_config?platform=eq.android' $a.Token @{ min_version_code = 9999 }
Check 'Un usuario NO puede modificar app_config' ((Count $r) -eq 0 -or (Denied $r)) "HTTP $($r.Status) $($r.Text)"
$r = Call 'PATCH' '/rest/v1/app_config?platform=eq.android' $null @{ min_version_code = 9999 }
Check 'anon NO puede modificar app_config' ((Count $r) -eq 0 -or (Denied $r)) "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/app_config' $a.Token @{ platform = 'android'; min_version_code = 1 }
Check 'Un usuario NO puede insertar en app_config' ((Denied $r) -or $r.Status -eq 409) "HTTP $($r.Status)"
$r = Call 'GET' '/rest/v1/app_config?platform=eq.android&select=min_version_code' $null
Check 'min_version_code de android no quedó en 9999' ($r.Json[0].min_version_code -ne 9999) $r.Text

Write-Host "`n== Límites del servidor (endurecimiento) =="
$big = @{ id = "$id-big"; note_date = '2026-01-01'; kind = 'Note'; body = ('x' * 5000); updated_at = $now }
$r = Call 'POST' '/rest/v1/day_notes' $a.Token $big
Check 'Nota de 5000 caracteres rechazada' ($r.Status -eq 400 -and $r.Text -match '23514|day_notes_len') "HTTP $($r.Status) $($r.Text)"
$far = @{ id = "$id-far"; note_date = '2026-01-01'; kind = 'Note'; body = 'rls-test'; updated_at = 9000000000000000 }
$r = Call 'POST' '/rest/v1/day_notes' $a.Token $far
$stored = if ($r.Status -eq 201) { [int64]$r.Json[0].updated_at } else { -1 }
$limit = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + 300000 + 60000
Check 'updated_at futuro se recorta a «ahora + 5 min»' ($r.Status -eq 201 -and $stored -le $limit) "HTTP $($r.Status), guardado=$stored"

Write-Host "`n== Alias, solicitudes y bloqueos =="
$socialTag = [guid]::NewGuid().ToString('N').Substring(0, 8)
$aliasA = "rlsa_$socialTag"; $aliasB = "rlsb_$socialTag"
$r = Call 'POST' '/rest/v1/rpc/set_my_alias' $a.Token @{ p_alias = $aliasA }
Check 'A fija su alias' ($r.Status -eq 200 -and $r.Json -eq $aliasA) "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/set_my_alias' $a.Token @{ p_alias = "otro_$socialTag" }
Check 'A NO puede cambiar su alias ya fijado' ($r.Text -match 'alias_locked') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/set_my_alias' $b.Token @{ p_alias = $aliasB }
Check 'B fija su alias' ($r.Status -eq 200) "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/request_link' $b.Token @{ p_alias = $aliasA }
Check 'B pide vínculo a A' ($r.Json -eq 'sent') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/request_link' $a.Token @{ p_alias = $aliasA }
Check 'Nadie puede pedirse a sí mismo' ($r.Json -eq 'self') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/request_link' $b.Token @{ p_alias = "no_existe_$socialTag" }
Check 'Alias inexistente responde alias_not_found' ($r.Json -eq 'alias_not_found') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/list_pending_requests' $a.Token @{}
Check 'A ve la solicitud pendiente de B con su alias' ((Count $r) -eq 1 -and $r.Json[0].requester_alias -eq $aliasB) $r.Text
$linkId = if ($r.Json) { $r.Json[0].link_id } else { 'sin-link' }
$r = Call 'POST' '/rest/v1/rpc/get_shared_schedule' $b.Token @{ p_owner = $a.Uid; p_from = '2026-02-01'; p_to = '2026-02-28' }
Check 'B NO ve el calendario de A antes de que acepte' ($r.Status -eq 400 -and $r.Text -match 'forbidden') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/set_link_status' $b.Token @{ p_link = $linkId; p_status = 'accepted' }
Check 'B NO puede aceptarse su propia solicitud' ($r.Text -match 'invalid_transition|invalid_status|forbidden') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/links' $b.Token @{ requester_id = $b.Uid; owner_id = $a.Uid; status = 'accepted' }
Check 'Nadie inserta vínculos directamente' ($r.Status -ge 400) "HTTP $($r.Status) $($r.Text)"
$r = Call 'PATCH' "/rest/v1/links?id=eq.$linkId" $b.Token @{ status = 'accepted' }
Check 'B NO puede cambiar el estado del vínculo con PATCH' ((Count $r) -eq 0) "HTTP $($r.Status) filas=$(Count $r)"
$r = Call 'GET' "/rest/v1/profiles?owner_id=eq.$($a.Uid)&select=alias" $b.Token
Check 'B NO lee el perfil de A directamente' ((Count $r) -eq 0) "filas=$(Count $r)"
$r = Call 'POST' '/rest/v1/rpc/set_link_status' $a.Token @{ p_link = $linkId; p_status = 'accepted' }
Check 'A acepta la solicitud' ($r.Status -in 200, 204) "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/get_shared_schedule' $b.Token @{ p_owner = $a.Uid; p_from = '2026-02-01'; p_to = '2026-02-28' }
Check 'Tras aceptar, B puede leer el calendario de A' ($r.Status -eq 200) "HTTP $($r.Status) $($r.Text)"

Write-Host "`n== Bloqueos =="
$r = Call 'POST' '/rest/v1/rpc/block_requester' $a.Token @{ p_link = $linkId }
Check 'A bloquea a B' ($r.Status -in 200, 204) "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/get_shared_schedule' $b.Token @{ p_owner = $a.Uid; p_from = '2026-02-01'; p_to = '2026-02-28' }
Check 'Tras bloquear, B pierde el acceso al calendario de A' ($r.Status -eq 400 -and $r.Text -match 'forbidden') "HTTP $($r.Status)"
$r = Call 'POST' '/rest/v1/rpc/request_link' $b.Token @{ p_alias = $aliasA }
Check 'B pide de nuevo y recibe sent (sin revelar el bloqueo)' ($r.Json -eq 'sent') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/list_pending_requests' $a.Token @{}
Check 'El bloqueado NO aparece como pendiente' ((Count $r) -eq 0) "filas=$(Count $r)"
$r = Call 'POST' '/rest/v1/rpc/list_blocked' $a.Token @{}
Check 'A ve a B en su lista de bloqueados' ((Count $r) -eq 1 -and $r.Json[0].blocked_id -eq $b.Uid) $r.Text
$r = Call 'POST' '/rest/v1/rpc/unblock_requester' $a.Token @{ p_blocked = $b.Uid }
Check 'A desbloquea a B' ($r.Status -in 200, 204) "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/request_link' $b.Token @{ p_alias = $aliasA }
Check 'Tras desbloquear, B puede volver a pedir vínculo' ($r.Json -eq 'sent') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/list_pending_requests' $a.Token @{}
Check 'La nueva solicitud de B aparece como pendiente' ((Count $r) -eq 1) "filas=$(Count $r)"
$linkId2 = if ($r.Json) { $r.Json[0].link_id } else { 'sin-link' }
$null = Call 'POST' '/rest/v1/rpc/set_link_status' $a.Token @{ p_link = $linkId2; p_status = 'accepted' }

Write-Host "`n== Propuestas de cambio de turno =="
$sid = "rlstest-sw-$socialTag"; $d1 = '2026-02-02'; $d2 = '2026-02-03'
$shiftRow = @{ id = $sid; name = 'rlsturno'; abbreviation = 'R'; color_argb = 1; sort_order = 98; updated_at = $now }
$r = Call 'POST' '/rest/v1/shifts' $a.Token $shiftRow
Check 'A tiene el turno de prueba' ($r.Status -eq 201) "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/shifts' $b.Token $shiftRow
Check 'B tiene el mismo turno de prueba' ($r.Status -eq 201) "HTTP $($r.Status) $($r.Text)"
# A trabaja d2 (lo que recibirá B); B trabaja d1 (lo que entregará)
$r = Call 'POST' '/rest/v1/shift_assignments' $a.Token @{ id = "$d2#0"; assignment_date = $d2; slot = 0; shift_id = $sid; updated_at = $now }
$r = Call 'POST' '/rest/v1/shift_assignments' $b.Token @{ id = "$d1#0"; assignment_date = $d1; slot = 0; shift_id = $sid; updated_at = $now }
$r = Call 'POST' '/rest/v1/rpc/propose_swap' $b.Token @{ p_target = $a.Uid; p_proposer_date = $d1; p_proposer_slot = 0; p_target_date = $d2; p_target_slot = 0 }
Check 'B propone el cambio a A' ($r.Status -eq 200 -and $r.Json) "HTTP $($r.Status) $($r.Text)"
$swapId = $r.Json
$r = Call 'POST' '/rest/v1/rpc/respond_swap' $b.Token @{ p_swap = $swapId; p_accept = $true }
Check 'B NO puede aceptar su propia propuesta' ($r.Text -match 'forbidden') "HTTP $($r.Status) $($r.Text)"
$r = Call 'POST' '/rest/v1/rpc/respond_swap' $a.Token @{ p_swap = $swapId; p_accept = $true }
Check 'A acepta el cambio' ($r.Status -eq 200 -and $r.Json -eq 'accepted') "HTTP $($r.Status) $($r.Text)"
$r = Call 'GET' "/rest/v1/shift_assignments?owner_id=eq.$($a.Uid)&assignment_date=eq.$d1&deleted_at=is.null&select=shift_id" $a.Token
Check 'Tras el cambio, A trabaja el día d1' ((Count $r) -eq 1) "filas=$(Count $r)"
$r = Call 'GET' "/rest/v1/shift_assignments?owner_id=eq.$($b.Uid)&assignment_date=eq.$d2&deleted_at=is.null&select=shift_id" $b.Token
Check 'Tras el cambio, B trabaja el día d2' ((Count $r) -eq 1) "filas=$(Count $r)"
$r = Call 'GET' "/rest/v1/shift_assignments?owner_id=eq.$($a.Uid)&assignment_date=eq.$d2&deleted_at=is.null&select=shift_id" $a.Token
Check 'A ya no trabaja el día d2' ((Count $r) -eq 0) "filas=$(Count $r)"
$r = Call 'POST' '/rest/v1/rpc/respond_swap' $a.Token @{ p_swap = $swapId; p_accept = $true }
Check 'Una propuesta no se puede aceptar dos veces' ($r.Text -match 'invalid_transition') "HTTP $($r.Status) $($r.Text)"

Write-Host "`n== Limpieza (borrado lógico de las filas de prueba de A) =="
$t = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + 10
$null = Call 'PATCH' "/rest/v1/day_notes?id=like.$id*" $a.Token @{ deleted_at = $t; body = ''; updated_at = $t }
$null = Call 'PATCH' "/rest/v1/shifts?id=like.$id*" $a.Token @{ deleted_at = $t; updated_at = $t }
$null = Call 'PATCH' "/rest/v1/shifts?id=eq.$sid" $a.Token @{ deleted_at = $t; updated_at = $t }
$null = Call 'PATCH' "/rest/v1/shifts?id=eq.$sid" $b.Token @{ deleted_at = $t; updated_at = $t }
$null = Call 'PATCH' "/rest/v1/shift_assignments?shift_id=eq.$sid" $a.Token @{ deleted_at = $t; updated_at = $t }
$null = Call 'PATCH' "/rest/v1/shift_assignments?shift_id=eq.$sid" $b.Token @{ deleted_at = $t; updated_at = $t }
if ($auto) {
    Write-Host "`nBorra las cuentas temporales en Supabase → SQL Editor (sus filas caen en cascada):"
    Write-Host "  delete from auth.users where email like 'rlstest-%@example.com';"
}
Write-Host ''
if ($script:fails -eq 0) { Write-Host 'RESULTADO: todo correcto, RLS y límites funcionan.' -ForegroundColor Green }
else { Write-Host "RESULTADO: $($script:fails) comprobación(es) fallida(s)." -ForegroundColor Red; exit 1 }
