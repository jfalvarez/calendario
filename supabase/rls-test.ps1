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
        $text = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd()
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

Write-Host "`n== Limpieza (borrado lógico de las filas de prueba de A) =="
$t = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + 10
$null = Call 'PATCH' "/rest/v1/day_notes?id=like.$id*" $a.Token @{ deleted_at = $t; body = ''; updated_at = $t }
$null = Call 'PATCH' "/rest/v1/shifts?id=like.$id*" $a.Token @{ deleted_at = $t; updated_at = $t }
if ($auto) {
    Write-Host "`nBorra las cuentas temporales en Supabase → SQL Editor (sus filas caen en cascada):"
    Write-Host "  delete from auth.users where email like 'rlstest-%@example.com';"
}
Write-Host ''
if ($script:fails -eq 0) { Write-Host 'RESULTADO: todo correcto, RLS y límites funcionan.' -ForegroundColor Green }
else { Write-Host "RESULTADO: $($script:fails) comprobación(es) fallida(s)." -ForegroundColor Red; exit 1 }
