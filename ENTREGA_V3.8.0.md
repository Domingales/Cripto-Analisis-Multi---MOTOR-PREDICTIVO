# Entrega V3.8.0 resultados predictivos V2

## Base y versión

- Base confirmada: V3.7.5 vigilancia real background.
- Nueva versión: `versionCode 20`, `versionName 3.8.0-resultados-predictivos-v2`.
- Aplicación: `com.domingales.criptoanalisis.multi`.

## Cambios funcionales

- `status` representa el resultado operativo por primer toque de objetivo o stop.
- `persistence_status` representa la persistencia obtenida mediante cuatro cierres adaptados al timeframe.
- `legacy_status` conserva la clasificación anterior al actualizar desde V3.7.5.
- Aprendizaje, probabilidad histórica y RENDIMIENTO usan sólo `EXACT_FIRST_TOUCH_5M_V2`.
- Las señales V3.7 exactas se marcan para reclasificación automática desde Binance sin borrar sus datos anteriores.
- Se almacenan motivo y momento del desenlace, umbral, objetivo, stop, MFE y MAE.
- El backtest utiliza primer toque y una ventana adaptada al timeframe.
- Se añadieron filtros de agotamiento, proximidad al nivel contrario, giro inmediato y contradicción del intervalo superior.
- El contexto BTC ya no aumenta una señal sólo por coincidir; se limita a penalizar contradicción y riesgo.

## Horizontes de persistencia

- 15m: 15m, 1h, 4h y 24h.
- 30m: 30m, 2h, 8h y 24h.
- 1h: 1h, 4h, 12h y 24h.
- 4h: 4h, 12h, 24h y 3 días.
- 1d: 1 día, 3 días, 7 días y 14 días.

## Conservación

- No se eliminan tablas, señales, snapshots, alertas ni seguimientos anteriores.
- La tabla `followup` queda como historial V1.
- La tabla nueva `persistence_followup` contiene la medición adaptada V2.
- No se modifican ForegroundService, FCM, Cloudflare Worker, alarmas de precio, notificaciones ni detector de explosiones.

## Verificación realizada en este entorno

- Estructura completa del ZIP comparada con la base V3.7.5; no hay archivos eliminados.
- Migración SQLite V9 a V10 simulada con una señal realista: conserva `legacy_status`, calcula objetivo/stop y crea los cuatro horizontes V2.
- Revisión de referencias antiguas y de las consultas de aprendizaje, rendimiento, historial y exportación.
- Pruebas unitarias añadidas para primer toque, ambigüedad intravela, horizontes, BUY/SELL, agotamiento, BTC y backtest adaptado.

La compilación Android y la ejecución de JUnit no pudieron completarse en este contenedor porque no dispone del SDK Android ni de Gradle 8.9 en caché y la descarga externa está bloqueada. Deben ejecutarse en Android Studio con JDK 17 antes de instalar.

## Prueba recomendada en Android Studio

1. Abrir la carpeta completa como proyecto.
2. Esperar la sincronización de Gradle.
3. Ejecutar `Run tests` o `gradlew.bat test`.
4. Ejecutar `Build > Build APK(s)`.
5. Instalar sobre V3.7.5 sin desinstalar ni borrar datos.
6. Seguir `PRUEBAS_RESULTADOS_PREDICTIVOS_V3.8.0.md`.
