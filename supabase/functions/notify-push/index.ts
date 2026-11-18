// Calendario · envío de avisos push (BORRADOR, sin desplegar).
// La llama send_push() desde la base de datos (supabase/social_push.sql) con el secreto en la cabecera x-notify-secret.
// Secretos de la función (supabase secrets set):
//   NOTIFY_SECRET            mismo valor que push_config.notify_secret
//   FCM_PROJECT_ID           id del proyecto de Firebase
//   FCM_CLIENT_EMAIL         client_email de la cuenta de servicio de Firebase
//   FCM_PRIVATE_KEY          private_key de la cuenta de servicio (con los \n escapados)
// SUPABASE_URL y SUPABASE_SERVICE_ROLE_KEY los inyecta Supabase.

import { createClient } from "npm:@supabase/supabase-js@2";
import { SignJWT, importPKCS8 } from "npm:jose@5";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

async function fcmAccessToken(): Promise<string> {
  const key = await importPKCS8(Deno.env.get("FCM_PRIVATE_KEY")!.replace(/\\n/g, "\n"), "RS256");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ scope: "https://www.googleapis.com/auth/firebase.messaging" })
    .setProtectedHeader({ alg: "RS256" })
    .setIssuer(Deno.env.get("FCM_CLIENT_EMAIL")!)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!res.ok) throw new Error(`oauth ${res.status}`);
  return (await res.json()).access_token as string;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method" }, 405);
  if (req.headers.get("x-notify-secret") !== Deno.env.get("NOTIFY_SECRET")) return json({ error: "forbidden" }, 403);

  const { user_id, title, body } = await req.json();
  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const { data: rows, error } = await db.from("device_tokens").select("token").eq("owner_id", user_id);
  if (error) return json({ error: "db" }, 500);

  const token = await fcmAccessToken();
  const url = `https://fcm.googleapis.com/v1/projects/${Deno.env.get("FCM_PROJECT_ID")}/messages:send`;
  let sent = 0;
  for (const row of rows ?? []) {
    const res = await fetch(url, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      body: JSON.stringify({ message: { token: row.token, notification: { title, body } } }),
    });
    if (res.ok) {
      sent++;
    } else {
      // Token caducado o desinstalado: se borra para no volver a intentarlo.
      if (res.status === 404 || res.status === 400) {
        await db.from("device_tokens").delete().eq("owner_id", user_id).eq("token", row.token);
      }
    }
  }
  return json({ sent });
});
