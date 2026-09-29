# Pruebas del refresco automático de alarmas — V3.7.3

## Objetivo

Comprobar que una alarma disparada por la vigilancia en segundo plano aparece como DISPARADA al volver a la pantalla Alarmas de precio, sin necesidad de pulsar la notificación.

## Prueba principal en móvil real

1. Crear una alarma de precio que pueda cumplirse de forma controlada y confirmar que aparece ACTIVA.
2. Enviar la aplicación a segundo plano y bloquear la pantalla.
3. Esperar una activación remota y la notificación de la alarma.
4. Desbloquear el móvil sin pulsar la notificación.
5. Abrir CriptoAnálisis Multi desde su icono o desde aplicaciones recientes.
6. Entrar en Alarmas de precio si no estaba ya visible.
7. Confirmar que la alarma aparece automáticamente como DISPARADA y muestra el último precio.

## Regresión

1. Crear una alarma y comprobar que los botones CANCELAR y MARCAR EXPIRADA siguen funcionando.
2. Reactivar una alarma disparada, cancelada o expirada y comprobar que vuelve a ACTIVA.
3. Confirmar que no hay parpadeo ni recarga repetitiva al permanecer en la pantalla.
4. Confirmar que cada alarma genera una sola notificación al dispararse.
5. Verificar que la vigilancia, las señales y las notificaciones continúan funcionando con la pantalla apagada.
