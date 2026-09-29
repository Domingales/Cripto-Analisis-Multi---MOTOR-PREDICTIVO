# Pruebas de aceptación — V3.1.0

## Preparación

1. Abre el proyecto completo en Android Studio con JDK 17.
2. Espera a que termine Gradle Sync.
3. Ejecuta `Build > Clean Project` y `Build > Rebuild Project`.
4. Ejecuta los tests de `app/src/test`.

## Punto 28 — mínimo estadístico

1. En una instalación nueva abre una ficha.
2. Comprueba que aparece `sin calibrar` y no `0%` ni una probabilidad técnica disfrazada.
3. En registros con 0–9 resultados HIT/FAIL comparables debe mantenerse `INSUFFICIENT`.
4. A partir de 10 resultados HIT/FAIL comparables puede aparecer probabilidad y confianza `LOW`.

## Puntos 31 y 32 — continuidad y reinicio

1. Activa la vigilancia; comprueba en Diagnóstico un ciclo `Inicio programado` y `Fin programado`.
2. Cierra la app y espera al menos 15 minutos con red disponible. Android puede desplazar la hora por ahorro de batería.
3. Reinicia el teléfono. No debe aparecer un error de inicio de ForegroundService desde BOOT_COMPLETED.
4. Tras el reinicio, confirma en Diagnóstico que vuelve a ejecutarse un ciclo programado.
5. Pulsa `ANALIZAR AHORA`: debe aparecer una notificación temporal y desaparecer al terminar el ciclo.
6. Si configuras la vía híbrida, verifica `/health`, guarda URL/clave y comprueba el evento `Activación remota recibida`.

## Punto 47 — intravela

1. Desmarca `Confirmar con VELA CERRADA`.
2. Ejecuta varios análisis durante una vela abierta.
3. Aunque supere el umbral, debe registrarse como `PRESIGNAL` con “pendiente de confirmación al cierre”.
4. No debe insertarse en la tabla de señales ni emitir alerta.
5. Activa vela cerrada y repite después del cierre: si supera todos los filtros, ahora sí puede producir `SIGNAL`.

## Punto 54 — backtest MTF

1. Abre `BACKTEST HISTÓRICO` y ejecuta un símbolo/timeframe.
2. Debe descargar cinco históricos y mostrar `Ventanas MTF válidas` mayor que cero cuando exista cobertura.
3. Las ventanas sin al menos dos marcos se cuentan como omitidas, no como no-señales.
4. Verifica que el resultado ya no queda siempre en cero por “confirmación multitemporal” incompleta.

## Limitaciones que deben mantenerse visibles

- Sin `google-services.json`, la app funciona localmente y el remoto queda pendiente.
- El intervalo periódico local mínimo efectivo es 15 minutos.
- Forzar la detención de la app puede impedir trabajos y mensajes hasta abrirla de nuevo.
- Las cifras del backtest no incluyen noticias históricas ni garantizan resultados futuros.
