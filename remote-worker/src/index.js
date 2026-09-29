const TOKEN_AUDIENCE = "https://oauth2.googleapis.com/token";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname === "/health") return json({ ok: true, service: "CriptoAnalisisMulti", mode: "hybrid-trigger" });
    if (request.method !== "POST" || !["/register", "/unregister"].includes(url.pathname)) return new Response("Not found", { status: 404 });
    if (request.headers.get("Authorization") !== `Bearer ${env.APP_REGISTRATION_SECRET}`) return new Response("Unauthorized", { status: 401 });
    const body = await request.json();
    if (!body.installationId || !body.token) return new Response("Invalid body", { status: 400 });
    const key = `device:${body.installationId}`;
    if (url.pathname === "/unregister") await env.DEVICES.delete(key);
    else await env.DEVICES.put(key, JSON.stringify({ token: body.token, updatedAt: Date.now() }));
    return json({ ok: true });
  },

  async scheduled(_event, env, ctx) {
    ctx.waitUntil(wakeRegisteredDevices(env));
  }
};

async function wakeRegisteredDevices(env) {
  const accessToken = await googleAccessToken(env);
  let cursor;
  let devices = 0;
  let accepted = 0;
  let failed = 0;

  do {
    const page = await env.DEVICES.list({ prefix: "device:", cursor });
    for (const key of page.keys) {
      const device = await env.DEVICES.get(key.name, "json");
      if (!device?.token) continue;
      devices += 1;

      const response = await fetch(`https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ message: { token: device.token, data: { type: "SCAN_REQUEST", requestedAt: String(Date.now()) }, android: { priority: "high", ttl: "3600s", collapse_key: "market-scan" } } })
      });

      const responseBody = await response.text();
      if (response.ok) {
        accepted += 1;
        console.log(`FCM_ACCEPTED status=${response.status}`);
      } else {
        failed += 1;
        console.error(`FCM_REJECTED status=${response.status} body=${safeLog(responseBody)}`);
      }

      if (response.status === 404 || response.status === 400) await env.DEVICES.delete(key.name);
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);

  console.log(`FCM_SUMMARY devices=${devices} accepted=${accepted} failed=${failed}`);
}

async function googleAccessToken(env) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64Url(JSON.stringify({ iss: env.FIREBASE_CLIENT_EMAIL, scope: FCM_SCOPE, aud: TOKEN_AUDIENCE, iat: now, exp: now + 3500 }));
  const unsigned = `${header}.${claims}`;
  const key = await crypto.subtle.importKey("pkcs8", pemBytes(env.FIREBASE_PRIVATE_KEY), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const assertion = `${unsigned}.${bytesBase64Url(new Uint8Array(signature))}`;
  const response = await fetch(TOKEN_AUDIENCE, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" }, body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }) });
  if (!response.ok) throw new Error(`OAuth ${response.status}`);
  return (await response.json()).access_token;
}

function safeLog(value) {
  return String(value || "(sin cuerpo)").replace(/[\r\n]+/g, " ").slice(0, 1000);
}
function pemBytes(pem) {
  const clean = pem.replace(/\\n/g, "\n").replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  return Uint8Array.from(atob(clean), c => c.charCodeAt(0));
}
function base64Url(text) { return bytesBase64Url(new TextEncoder().encode(text)); }
function bytesBase64Url(bytes) { return btoa(String.fromCharCode(...bytes)).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_"); }
function json(value) { return new Response(JSON.stringify(value), { headers: { "Content-Type": "application/json" } }); }
