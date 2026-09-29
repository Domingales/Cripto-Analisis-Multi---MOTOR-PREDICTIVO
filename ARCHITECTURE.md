# Arquitectura — CriptoAnálisis Multi 3.8.0

## Flujo principal

ForegroundService local / activación FCM / JobScheduler de respaldo
→ coordinador MarketWatchService
→ MarketScanRunner
→ MarketRepository
→ BinanceMarketDataSource / NewsSentimentRepository
→ TechnicalEngine
→ contexto MTF + BTC + calibración histórica
→ SignalEngine
→ AppDatabase
→ NotificationHelper
→ resultado operativo por primer toque + persistencia adaptada
→ Performance / History / Diagnostics.

Después de persistir una evaluación cerrada, y sólo si el ajuste está activo, una copia pasa en paralelo a `ExplosionDetector`. Este módulo no devuelve datos a `SignalEngine`: registra exclusivamente detecciones 5/5 en sus propias tablas, emite su propia notificación y resuelve su resultado a ±1 ATR.

En paralelo, las alarmas de precio se evalúan con el precio actual de cada símbolo durante cada ciclo finito. Una condición que deja de cumplirse reinicia su contador sostenido; sólo se dispara cuando permanece cumplida todo el intervalo `D:HH:MM`.

## Estados auditables

1. `EVALUATION`: análisis completo sin cercanía suficiente al umbral.
2. `PRESIGNAL`: oportunidad cercana al umbral, todavía sin alerta.
3. `SIGNAL`: filtros técnicos superados.
4. `ALERT`: la señal única ya se persistió y se emite notificación.
5. `RESULT`: el seguimiento termina en HIT/FAIL/NEUTRAL.

El `MarketWatchService` permanece en primer plano mientras la vigilancia está activa. Mantiene un latido, solicita ciclos locales con el intervalo configurado, recibe activaciones FCM y utiliza `START_STICKY` para que Android pueda restaurarlo si elimina el proceso. “Analizar ahora” usa el mismo coordinador sin crear un motor paralelo.

## Persistencia

SQLite versión 10 con migración desde versiones anteriores. Además de análisis, señales y seguimiento, persiste activos/configuración, velas, snapshots técnicos completos, caché de indicadores, alarmas de precio y una outbox de notificaciones. La V10 conserva la clasificación anterior en `legacy_status`, reclasifica las señales exactas por primer toque y crea `persistence_followup` para horizontes adaptados. Las vistas `signal_outcome` e `historical_pattern` separan resultado operativo y persistencia sin duplicar registros.

El alta de una señal confirma en una sola transacción `signal` + `signal_snapshot` + cuatro seguimientos adaptados + `alert_event` + `alert_outbox`. La notificación Android se envía después del commit y queda marcada como `SENT`; si falla, permanece recuperable.

## Concurrencia

Un coordinador serial y una concesión persistida evitan ciclos solapados incluso tras recrear el proceso. Un pool fijo de dos workers analiza símbolos/timeframes de forma paralela controlada. Cada ciclo mantiene un wakelock temporal con límite de diez minutos y lo libera al terminar.

## Continuidad Android

- El servicio de tipo `specialUse` representa la función central de vigilancia activada expresamente por el usuario y evita el límite temporal propio de `dataSync` en Android 15.
- `JobScheduler` conserva una tarea de red persistente de respaldo con mínimo efectivo de 15 minutos.
- `BOOT_COMPLETED` y `MY_PACKAGE_REPLACED` restauran servicio, configuración, registro remoto y respaldo cuando la vigilancia estaba activa.
- Cloudflare Workers Free solicita ciclos mediante FCM/Firebase Spark. El mensaje conserva una hora, colapsa activaciones antiguas equivalentes y puede permanecer pendiente hasta una hora.
- Antes de iniciar el servicio desde segundo plano se comprueba que FCM conserve prioridad alta; si Firebase la degrada, se utiliza JobScheduler y se registra el hecho en Diagnóstico.
- Si el usuario fuerza la detención desde Ajustes, Android bloquea servicios, receptores y FCM hasta abrir otra vez la aplicación.

## Datos

- Histórico OHLCV: Binance Spot público USDT.
- Precio actual de seguimiento: ticker independiente.
- Noticias: RSS best-effort, no bloqueante y con peso limitado.
- Validación: orden temporal, OHLC coherente, volumen, gaps, frescura, timeouts, retry y backoff.
- Optimización: descarga completa inicial; después se sincronizan las últimas velas y se fusionan con `candle_cache`. En vela cerrada, un indicador ya calculado para el mismo cierre se reutiliza desde `indicator_cache`.

## Aprendizaje y auditoría

Los casos comparables se buscan exclusivamente entre señales con snapshot completo, resultado operativo final y calidad `EXACT_FIRST_TOUCH_5M_V2`. Se comparan cripto, timeframe, dirección, régimen, estructura, RSI, ADX, ATR%, volumen, alineación EMA, proximidad S/R, MTF y régimen BTC. La persistencia y los resultados anteriores se conservan para auditoría, pero quedan fuera del aprendizaje.

El seguimiento parte del `closeTime` exacto que fijó el precio de entrada. Sólo admite velas de 5 minutos cuyo `openTime` sea posterior a ese cierre. El resultado operativo termina cuando objetivo o stop se toca primero; la persistencia toma cierres en ventanas adaptadas. MFE/MAE se limita a la ventana máxima de cada timeframe. Si objetivo y stop aparecen en una misma vela se clasifica neutral porque el orden intravela no es demostrable.

El backtest es walk-forward: reconstruye MTF y régimen BTC con datos ya cerrados en cada fecha, y una señal simulada no se convierte en caso comparable hasta finalizar su ventana adaptada. Si no se aporta una cronología histórica de noticias, ese componente queda neutral y la limitación se muestra; nunca se reutilizan noticias actuales en el pasado.

La dirección se determina primero sin componente histórica. Después se consultan comparables del mismo lado BUY/SELL y se reevalúa; si el histórico cambiara el lado, se recalibra con ese lado. Una oscilación descarta la componente histórica para impedir mezclar muestras opuestas.

## Señales

El UID usa símbolo + timeframe + cierre de vela + dirección. Además hay cooldown y exigencia de mejora de confianza para una repetición temprana. El sistema no alerta por score alto si fallan tendencia, volumen, MTF, dirección permitida o calidad temporal.

En modo intravela cada ciclo produce dos evaluaciones independientes: la vela abierta sólo puede ser PRESEÑAL y la última vela cerrada puede confirmar SIGNAL. Así el modo intravela no bloquea indefinidamente las señales definitivas.

## Interfaz y notificaciones

Todas las pantallas usan el mismo encabezado negro/dorado y el menú global. Su posición se guarda en preferencias. Las alertas se encaminan a uno de cuatro canales Android (sonido+vibración, sólo sonido, sólo vibración o silencioso) según la configuración efectiva de cada símbolo. El catálogo de selección se actualiza desde `exchangeInfo` y conserva localmente la última lista utilizable.

Las posibles explosiones disponen de otros cuatro canales Android y ajustes globales propios. La etiqueta indica dirección y 5/5, pero siempre conserva el texto `sin calibrar` hasta que su historial independiente permita medir una precisión real.
