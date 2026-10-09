# Corrección del cooldown ante datos futuros ausentes

Estado: implementada en rama local `fix/backtest-future-availability`, pendiente de compilación y ejecución de las nuevas pruebas Kotlin. No integrada ni desplegada.

Base: `ee800503c61443ec18f73e75b76f1ed3322a1733` de main. Se revisó el árbol del proyecto, las llamadas desde BacktestActivity, las pruebas de backtesting y la referencia de los 59 puntos. El cambio corresponde a los puntos 26 (PENDIENTE), 34 (cooldown) y 54 (backtesting cronológico), conservando el resto del motor, interfaz, persistencia, alarmas y vigilancia.

## Problema y reparación

Después de aceptar una señal, el motor comprueba el futuro de 5 minutos. Si falta una vela, guarda un caso PENDIENTE y continúa sin registrar el cooldown. Una ausencia futura podía, por tanto, aumentar las aceptaciones anteriores a esa ausencia.

La asignación `lastAccepted[side] = asOf to analysis.confidence` se registra inmediatamente después de `onAccepted`, antes de consultar el futuro. Se conservan las asignaciones posteriores redundantes para minimizar el cambio. El comportamiento con datos completos permanece algebraicamente igual; una ventana incompleta sigue como PENDIENTE/MISSING_5M_DATA. No se modifican score, umbrales, indicadores ni reglas de resolución.

Archivos modificados/creados:

- `app/src/main/java/com/domingales/criptoanalisis/multi/domain/BacktestEngine.kt`: una asignación y un comentario.
- `app/src/test/java/com/domingales/criptoanalisis/multi/domain/BacktestFutureAvailabilityTest.kt`: dos pruebas de invariancia al eliminar una vela futura y al cambiar precios futuros. La prueba de huecos exige casos PENDIENTE y aceptaciones anteriores no vacías.
- Este documento. No se elimina ningún archivo ni se cambia la versión de una APK publicada.

## Validación y límites

`git diff --check` pasa. El mismo cambio semántico fue probado previamente en la copia de investigación de PR #9; esto no sustituye la ejecución de las pruebas de esta rama.

El intento local `bash ./gradlew --no-daemon testDebugUnitTest assembleDebug` se detuvo descargando Gradle 8.9: `java.net.UnknownHostException: services.gradle.org`. No llegó a compilar ni ejecutar pruebas Android. La subida a GitHub para ejecutar CI fue rechazada por revisión automática: interpretó «sin publicar» como prohibición de subir el código al repositorio público. No se vuelve a intentar sin autorización explícita.

Para comprobar en Windows 11, aplicar el parche a la base indicada o abrir esta rama completa y ejecutar en la terminal de Android Studio:

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest assembleDebug
```

Revisar específicamente las dos pruebas de `BacktestFutureAvailabilityTest`, las pruebas existentes de BacktestEngine/OutcomeTracker y el informe de errores completo. No instalar ni distribuir una APK hasta que compile y pasen las pruebas. No se han efectuado pruebas físicas en el DOOGEE, ni de Cloudflare/Firebase: el cambio no toca esos componentes.
