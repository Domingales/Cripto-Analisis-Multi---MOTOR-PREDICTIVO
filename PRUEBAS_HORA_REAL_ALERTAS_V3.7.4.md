# Pruebas V3.7.4 — Hora real de las alertas

## Objetivo

Comprobar que una notificación conserva la hora real del evento de mercado aunque Android la publique o recupere posteriormente.

## Reglas implementadas

- Señal COMPRA/VENTA: `candle_close` de la vela que produjo la señal.
- Alerta recuperada: `candle_close` persistido en `signal_snapshot`.
- Posible explosión: `candle_close` persistido en `explosion_event`.
- Alarma de precio: `triggered_ts`, momento de confirmación del cruce sostenido.
- Presentación: zona horaria local configurada en Android.

## Comprobación en el DOOGEE

1. Instalar la APK sobre la versión existente, sin desinstalarla.
2. Mantener activa la vigilancia y comprobar que la pantalla principal muestra `CriptoAnálisis Multi 3.7.4`.
3. Ante una nueva señal, desplegar completamente la notificación.
4. Verificar que el texto incluye `Señal DD/MM/AAAA HH:mm`.
5. Comparar esa hora con el cierre de vela registrado en Historial o Diagnóstico.
6. Para una alarma de precio, comprobar que aparece `Alarma DD/MM/AAAA HH:mm` y coincide con su hora de disparo.
7. Si se recupera una alerta pendiente, confirmar que mantiene la hora original y no adopta la hora de recuperación.

## Regresión mínima

- Abrir una notificación de señal y comprobar que accede a la ficha de la criptomoneda.
- Abrir una alarma de precio y comprobar que accede a Alarmas de precio.
- Confirmar que sonido y vibración respetan la configuración por criptomoneda.
- Confirmar que no aumenta el número de señales o alertas por instalar esta versión.
- Confirmar que continúan los ciclos locales y las activaciones remotas.

## Estado de verificación

- Revisión estática de timestamps y consultas SQLite: realizada.
- Sintaxis del Worker: correcta y archivo idéntico al de V3.7.3.
- Compilación Android: intentada, pero bloqueada antes de cargar el proyecto porque el entorno no dispone de Gradle 8.9 ni puede descargarlo de `services.gradle.org`. Pendiente en Android Studio.
- Prueba física en DOOGEE: pendiente del usuario.
