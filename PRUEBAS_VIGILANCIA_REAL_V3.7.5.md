# Pruebas V3.7.5 vigilancia real en segundo plano

## Instalación

1. Compilar e instalar sobre la versión anterior sin desinstalar.
2. Abrir la app, conceder notificaciones y pulsar AJUSTES DE BATERÍA.
3. En el DOOGEE permitir ejecución sin restricciones e inicio automático.
4. Mantener activa la vigilancia y comprobar la notificación fija.

## Prueba principal

1. Anotar contadores y hora del último ciclo en Diagnóstico.
2. Bloquear el móvil durante al menos 35 minutos sin activar No molestar.
3. No abrir la app durante la prueba.
4. Desbloquear y abrir Diagnóstico detallado.
5. Confirmar ciclos REMOTO_FCM o LOCAL_FGS durante el periodo bloqueado, sin huecos prolongados y sin pares de ciclos separados por segundos.
6. Crear antes una alarma de precio de prueba alcanzable para comprobar aviso con pantalla bloqueada.

## Casos adicionales

- Retirar la app de recientes: la notificación fija y los ciclos deben continuar.
- Reiniciar el móvil: la vigilancia debe restaurarse si estaba activada.
- Silencio o No molestar: el ciclo y la notificación deben registrarse sin forzar sonido ni vibración.
- Forzar detención: se espera que Android detenga todo hasta abrir otra vez la app.
- Diagnóstico: ForegroundService debe figurar RESPONDIENDO y mostrar origen, inicio, fin y retraso FCM.

## Worker

Después de desplegar `remote-worker`, `wrangler tail` debe seguir mostrando FCM_ACCEPTED. El proyecto usa TTL 3600 segundos y `collapse_key=market-scan`, por lo que Android no debe liberar una ráfaga de activaciones antiguas.

## Publicación posterior

Antes de subir un AAB a Google Play, declarar el ForegroundService `specialUse` y copiar la finalidad indicada en el manifiesto. Esta revisión de Play Console es independiente de la compilación e instalación local por APK.
