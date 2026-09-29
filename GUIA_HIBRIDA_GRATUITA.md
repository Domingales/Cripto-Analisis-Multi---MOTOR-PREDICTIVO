# Continuidad híbrida gratuita V3.7.5 — puntos 30, 31 y 32

Esta versión mantiene un ForegroundService `specialUse` mientras el usuario activa la vigilancia. El servicio coordina ciclos locales con el intervalo elegido, FCM como activación remota y JobScheduler como respaldo recuperable de 15 minutos o más. La notificación fija de vigilancia es obligatoria para que Android identifique el trabajo continuo.

## Qué funciona sin configurar cuentas

- Activar vigilancia inicia el coordinador permanente, ejecuta un ciclo y programa los siguientes.
- Retirar la interfaz de recientes no detiene el coordinador.
- Reiniciar o actualizar el teléfono restaura el servicio y el trabajo de respaldo si la vigilancia seguía activada.
- El modo intravela nunca notifica una señal definitiva.

## Activar Cloudflare Workers Free + Firebase Spark

1. Crea un proyecto Firebase nuevo, plan **Spark**. No habilites Blaze ni añadas tarjeta.
2. Añade una app Android con paquete `com.domingales.criptoanalisis.multi`.
3. Descarga `google-services.json` y colócalo en `app/google-services.json`. El plugin solo se activa si ese archivo existe.
4. En Firebase Cloud Messaging, habilita la API HTTP v1 y crea una cuenta de servicio limitada al envío FCM. Conserva `project_id`, `client_email` y `private_key`; no los copies al proyecto Android.
5. En Cloudflare Free crea un KV y copia `remote-worker/wrangler.toml.example` como `wrangler.toml`; sustituye el ID del KV.
6. Desde `remote-worker`, ejecuta `npm install`, inicia sesión con `npx wrangler login` y carga secretos:
   - `npx wrangler secret put APP_REGISTRATION_SECRET`
   - `npx wrangler secret put FIREBASE_PROJECT_ID`
   - `npx wrangler secret put FIREBASE_CLIENT_EMAIL`
   - `npx wrangler secret put FIREBASE_PRIVATE_KEY`
7. Despliega con `npm run deploy`. En V3.7.5 este paso también aplica el TTL de una hora y el colapso de activaciones antiguas equivalentes. Comprueba `https://TU_WORKER.workers.dev/health`.
8. En Configuración > Continuidad híbrida, introduce la URL HTTPS y la misma `APP_REGISTRATION_SECRET`.

## Límites honestos

El servicio permanente y FCM mejoran sustancialmente la continuidad, pero Android y el fabricante todavía pueden restringir red o procesos. En el DOOGEE debe configurarse CriptoAnálisis Multi como aplicación sin restricciones de batería y permitirse el inicio automático. Si el usuario pulsa Forzar detención, ninguna app Android puede recibir FCM ni ejecutar servicios hasta abrirla de nuevo. El modo Silencio o No molestar conserva la notificación, pero el sistema decide si reproduce sonido o vibración.

Al publicar una actualización en Google Play hay que declarar el tipo de servicio en `Política > Contenido de la aplicación > Servicios en primer plano`. El subtipo `specialUse` se justifica como vigilancia continua de mercados y alarmas solicitada por el usuario; Google revisa esta explicación. No se ha añadido permiso de alarmas exactas ni ninguna función que eluda las restricciones del sistema.

No subas `google-services.json`, `wrangler.toml` ni secretos a repositorios públicos.
