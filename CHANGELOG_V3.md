# Cambios V3.0 visible-59points

## V3.7.5 vigilancia real en segundo plano

- ForegroundService coordinador permanente mientras la vigilancia está activada, con `START_STICKY`, notificación fija y ciclo local según el intervalo elegido.
- Activaciones FCM entregadas directamente al coordinador; JobScheduler permanece como respaldo de 15 minutos o más.
- Concesión persistente y coordinador serial para impedir ciclos duplicados entre orígenes LOCAL_FGS, REMOTO_FCM, JOB_RESPALDO, MANUAL y RECUPERACION.
- Wakelock parcial limitado exclusivamente a la duración de cada análisis, con liberación garantizada.
- Restauración tras reinicio o actualización de la app, continuidad al retirar la interfaz de recientes y acceso a los ajustes de batería.
- Diagnóstico ampliado: latido del servicio, último origen, inicio y fin, prioridad y retraso FCM, ciclos omitidos y optimización de batería.
- Comprobación de prioridad FCM antes de iniciar el servicio desde segundo plano; los mensajes degradados pasan al respaldo permitido por Android.
- Worker con TTL de una hora y `collapse_key` para conservar una única activación reciente durante reposo prolongado.
- No se modifican motor técnico, indicadores, umbrales, señales, aprendizaje, base SQLite, alarmas de precio ni apariencia general.
- versionCode 19 / versionName 3.7.5-vigilancia-real-background.

## V3.7.4 hora real de las alertas

- Las notificaciones de señal usan como hora principal el cierre de la vela que originó la señal, no la hora en que Android consiguió publicarlas.
- Las alertas recuperadas desde el outbox conservan el mismo cierre de vela y lo incluyen explícitamente en el texto de la notificación.
- Las posibles explosiones usan el cierre de la vela que confirmó las cinco condiciones.
- Las alarmas de precio usan `triggered_ts`, es decir, el momento en que se confirmó el cruce sostenido configurado.
- Las horas se presentan con la zona horaria local del teléfono y también se asignan a la marca temporal nativa de Android mediante `setWhen`.
- No se modifica el motor técnico, los criterios de señal, el esquema SQLite, Firebase, Cloudflare ni la planificación de la vigilancia.
- versionCode 18 / versionName 3.7.4-hora-real-alertas.

## V3.7.3 refresco automático de alarmas

- La pantalla Alarmas de precio vuelve a consultar SQLite al regresar a primer plano y actualiza automáticamente los cambios realizados por la vigilancia en segundo plano.
- El refresco sólo reconstruye la pantalla cuando cambia el estado, el armado, el disparo o el último precio de alguna alarma; evita recargas continuas y bucles de recreación.
- Se conserva el Worker con diagnóstico FCM seguro ya validado físicamente, sin mostrar tokens ni secretos.
- Se retira del proyecto la copia antigua `remote-worker/src/index.js .bak` para impedir que se restaure por error una versión que ocultaba los rechazos de Firebase.
- No se modifican el motor técnico, las señales, los porcentajes, el aprendizaje, el esquema SQLite, el Cron, Firebase ni el enlace KV.
- versionCode 17 / versionName 3.7.3-refresco-alarmas.

## V3.7.2 menú dorado ampliado

- El pequeño carácter `☰` se sustituye por un icono vectorial real con tres líneas horizontales doradas.
- El botón superior pasa a utilizar dimensiones independientes de la densidad de pantalla y queda aproximadamente al doble de tamaño visible.
- El desplegable estándar de Android se sustituye por un panel propio desplazable.
- Las catorce opciones conservan sus destinos y muestran fondo negro, marco dorado, texto dorado y esquinas redondeadas.
- Se mantiene la posición configurable izquierda/derecha y no se modifica ningún motor ni dato persistente.
- versionCode 16 / versionName 3.7.2-menu-dorado-ampliado.

## V3.7.1 navegación principal en menú hamburguesa

- Los once accesos del bloque inferior `Centro de control` pasan al menú hamburguesa global con sus nombres completos.
- Se incorpora al menú el acceso que faltaba a `MAPA DE LOS 59 PUNTOS`.
- El bloque inferior desaparece de la pantalla inicial para reducir desplazamiento y dejar el radar como contenido final.
- El menú se sitúa a la derecha una vez al actualizar a V3.7.1; después sigue siendo configurable desde Ajustes, manteniendo el punto 40.
- Los controles contextuales `ACTIVAR/DETENER VIGILANCIA`, `ANALIZAR AHORA`, `ORDENAR` y `VER TODAS LAS OPORTUNIDADES` permanecen junto al contenido que controlan.
- No se modifican motores, señales, porcentajes, aprendizaje, base de datos, detector de explosiones, alarmas, notificaciones ni vigilancia híbrida.
- versionCode 15 / versionName 3.7.1-menu-hamburguesa.

## V3.7.0 detector independiente de posibles explosiones

- Nuevo `ExplosionDetector`, ajeno a `SignalEngine`, que únicamente lee evaluaciones de vela cerrada y exige 5/5 condiciones simultáneas.
- Umbrales iniciales explícitos: volumen ≥2x; cierre más allá de soporte/resistencia; ATR ≥1,10x de la media reciente y superior al anterior; ADX ≥25 y creciente con DMI coherente; al menos tres intervalos MTF alineados, incluido el principal y uno superior, sin contradicciones.
- Persistencia separada en `explosion_event` y `explosion_alert_outbox`, con UID propio, deduplicación por cierre y recuperación de notificaciones fallidas.
- Seguimiento propio: objetivo +1 ATR e invalidación −1 ATR (invertidos para bajista), primer toque cronológico, vela ambigua NEUTRA, MFE/MAE y horizonte adaptado al timeframe.
- Pantalla independiente con totales, pendientes, aciertos, fallos, neutras, precisión resuelta, historial y exportación TXT, CSV y Copiar/Pegar JSON.
- Etiqueta 5/5 en el radar y la ficha sin sustituir la recomendación BUY/SELL/WAIT ni sus porcentajes.
- Controles separados para activar el detector, sonido y vibración.
- Base SQLite V9 con migración aditiva desde V1–V8, sin reescribir tablas existentes.
- versionCode 14 / versionName 3.7.0-explosion-independiente.

## V3.6.0 cierre estadístico — puntos 26, 27, 29 y 54

- Punto 26: el precio y todos los horizontes comparten el cierre confirmado como origen; MFE/MAE excluye cualquier vela iniciada antes o durante la señal.
- Punto 27: sólo los resultados `EXACT_POST_SIGNAL_5M` alimentan comparables; los V3.5 se conservan como `LEGACY_MISALIGNED_5M`.
- Punto 29: Rendimiento añade desgloses BUY/SELL, régimen y riesgo a total, cripto y timeframe.
- Punto 54: backtest walk-forward con MTF, contexto BTC histórico, preferencias BUY/SELL y calibración formada únicamente por señales cuyo horizonte ya había concluido.
- Las noticias actuales nunca se proyectan al pasado; si no existe cronología histórica, el backtest declara y neutraliza ese componente.
- Base SQLite V8 con migración conservadora desde V1–V7.
- versionCode 13 / versionName 3.6.0-cierre-estadistico.

## V3.5.0 resultados fiables — puntos 26, 27, 29, 53 y 54

- Punto 26: cada revisión 15m/1h/4h/24h se resuelve contra la última vela cerrada de 5 minutos anterior a su vencimiento; se conserva su timestamp de mercado. MFE/MAE usa máximos y mínimos OHLC y se limita al horizonte máximo de 24 horas.
- Punto 27: comparables y probabilidad usan exclusivamente resultados `EXACT_5M`; el histórico anterior se conserva como `LEGACY_SAMPLED` sin contaminar el aprendizaje.
- Punto 29: rendimiento global, por cripto y por timeframe calcula solo resultados exactos y muestra el número de registros heredados excluidos.
- Punto 53: exportación global TXT/CSV/RTF por flujo, en segundo plano, con configuración sincronizada, progreso visible y compartición segura por archivo; el portapapeles aplica un límite explícito.
- Punto 54: backtest con horizonte real de 24 horas por timeframe, primer toque cronológico de objetivo/stop, vela ambigua neutral y el mismo cooldown/mejora mínima del motor.
- Base SQLite V7 con migración conservadora desde V1–V6.
- versionCode 12 / versionName 3.5.0-resultados-fiables.

## V3.4.0 cierre auditable — puntos 15, 25, 27, 47 y 53

- Punto 15: nuevo régimen neutral `MIXED`; `POSSIBLE_REVERSAL` exige divergencia o falsa ruptura.
- Punto 25: snapshot V6 exacto con RSI avanzado, rupturas, zonas S/R, diez componentes, aprendizaje, noticias y configuración de decisión.
- Punto 27: calibración basada en la dirección técnica y recalibrada si el histórico cambia el lado final.
- Punto 47: evaluación dual; vela abierta para PRESEÑAL y última cerrada para SIGNAL dentro del mismo ciclo.
- Punto 53: exportación global y por ficha de configuración, evaluaciones, señales, snapshots, seguimientos, alertas, alarmas de precio y diagnóstico; portapapeles, compartir, TXT, CSV/Excel y RTF/Word.
- Caché de indicadores versionada para no reutilizar clasificaciones anteriores al nuevo régimen `MIXED`.
- Base SQLite V6 con migración conservadora desde V1–V5.
- versionCode 11 / versionName 3.4.0-cierre-auditable.

## V3.3.0 experiencia completa — puntos 1, 3, 4, 33, 36, 37, 38, 40 y 42

- Punto 1: acceso central a selección, intervalos, análisis, alarmas, historial, gráficos y exportación.
- Punto 3: catálogo Binance Spot USDT dinámico, búsqueda, selección múltiple y favoritos persistentes.
- Punto 4: configuración por criptomoneda con sonido y vibración independientes.
- Punto 33: cuatro canales reales de notificación y avisos con dirección, confianza, probabilidad/casos, riesgo y precio.
- Punto 36: dashboard con variación, ordenación persistente, última alerta, alarmas activas y seguimientos.
- Punto 37: estado “EN SEGUIMIENTO” morado separado de señal, preseñal y no operar.
- Punto 38: ficha ampliada con gráfico, EMA20/50/200, DMI, volumen, ATR y zonas de soporte/resistencia.
- Punto 40: menú hamburguesa en todas las pantallas y posición izquierda/derecha configurable.
- Punto 42: diagnóstico de red, API Binance, último ciclo, última evaluación y salud por criptomoneda.
- Alarmas de precio con condición mayor/menor, duración sostenida `D:HH:MM`, observación y estados Activa/Disparada/Cancelada/Expirada.
- Base SQLite V5 con migración conservadora desde V1–V4.
- versionCode 10 / versionName 3.3.0-experiencia-completa.

## V3.2.0 datos, aprendizaje y auditoría — puntos 22, 25, 27, 44, 46, 53 y 54

- Punto 22: señal, snapshot, seguimientos, evento de alerta y outbox se insertan en una sola transacción. Las notificaciones fallidas pueden recuperarse.
- Punto 25: cada nueva señal conserva el snapshot exacto de indicadores, estructura, MTF, contexto, previsión e invalidación usados en la decisión.
- Punto 27: calibración comparable avanzada por RSI, ADX, ATR%, EMA, volumen, estructura, S/R, MTF, contexto BTC, cripto, timeframe, régimen y dirección.
- Punto 44: base SQLite V4 con activos, configuración, velas, análisis, snapshots, señales/resultados, seguimientos, alertas/outbox, caché de indicadores y diagnóstico.
- Punto 46: `candle_cache` deja de ser decorativa; sincroniza las últimas velas, reutiliza indicadores de la misma vela cerrada y actualiza EMA/RSI en O(1) al aparecer una vela consecutiva.
- Punto 53: CSV/TXT global con todos los campos técnicos esenciales y ficha individual con snapshot por señal.
- Punto 54: backtest MTF sin look-ahead con desglose independiente BUY y SELL.
- Se activa AndroidX en `gradle.properties` para corregir la compilación con Firebase.
- versionCode 9 / versionName 3.2.0-datos-aprendizaje.

## V3.1.0 motor crítico — puntos 28, 31, 32, 47 y 54

- Punto 28: la probabilidad queda como “sin calibrar” hasta reunir al menos 10 resultados comparables resueltos; la confianza insuficiente nunca se mezcla con confianza técnica para fabricar un porcentaje.
- Puntos 31–32: vigilancia periódica mediante JobScheduler persistente, restauración segura tras reinicio/actualización y activación remota opcional Cloudflare Workers Free + Firebase Spark. El ForegroundService queda limitado al análisis manual iniciado por el usuario.
- Punto 47: el modo intravela nunca produce SIGNAL ni alerta; como máximo genera PRESIGNAL pendiente de cierre.
- Punto 54: backtest MTF real con cinco series, alineación por closeTime, exclusión de velas futuras y métricas de ventanas válidas/omitidas.
- Se conserva íntegramente la exportación de ficha completa de V3.0.4.
- versionCode 8 / versionName 3.1.0-motor-critico.

## V3.0.4 corrección de exportación

- Eliminada la referencia directa a `BuildConfig`, que no se genera en la configuración actual del proyecto.
- La versión incluida en los informes se obtiene ahora de forma segura mediante Android `PackageManager`.
- Se conserva íntegramente la exportación de la ficha completa incorporada en V3.0.3.
- versionCode 7 / versionName 3.0.4-correccion-exportacion.

## V3.0.3 exportación de ficha completa

- Nuevo botón `EXPORTAR FICHA COMPLETA` en la ficha individual de cada criptomoneda.
- Copia al portapapeles y uso del menú Compartir de Android.
- Guardado directo como TXT mediante el selector de documentos de Android.
- Guardado como CSV UTF-8 compatible con Excel, con separador por punto y coma.
- El informe incluye decisión, predicción, indicadores, MTF, Indicador Maestro, aprendizaje histórico, auditoría, evaluaciones recientes y seguimiento de señales.
- No requiere permisos adicionales de almacenamiento ni modifica el motor, las señales, las alarmas o la vigilancia.
- Se incorpora `PRUEBAS_EXPORTACION_FICHA.md` con instrucciones de instalación y comprobación.

## V3.0.2 consolidación

- El contador de ciclos muestra ciclos finalizados, no la suma de los eventos de inicio y fin.
- El contador de duplicados/cooldown muestra únicamente los generados desde la primera evaluación válida de la versión 3.0.1; se conserva el histórico anterior en la base de datos.
- La explicación del motor se presenta como una lista legible en vez del texto JSON interno.
- La invalidación estructural queda limitada entre 1,15 y 3 ATR para evitar niveles desproporcionados provocados por zonas antiguas o lejanas.
- Se actualizan los títulos visibles y la versión a 3.0.2.
- versionCode 5 / versionName 3.0.2-consolidacion.

## V3.0.1 integridad funcional

- Los registros heredados sin ATR, MTF o desglose verificable se conservan, pero ya no se presentan como análisis válidos ni alimentan la calibración.
- Una señal o preseñal requiere precio, ATR, volumen, medias y confirmación multitemporal válidos.
- En modo vela cerrada sólo se registra una evaluación por símbolo, intervalo y cierre de vela.
- Las señales ya no se contabilizan también como preseñales.
- Se recupera correctamente la puntuación MTF en la ficha individual.
- Las pantallas distinguen entre señal, preseñal y candidato que debe considerarse NO OPERAR.
- Los campos sin calibración, rango o invalidación dejan de mostrarse con porcentajes o ceros engañosos.
- El histórico antiguo sin ATR queda fuera del seguimiento, rendimiento, exportación y aprendizaje estadístico verificables.
- versionCode 4 / versionName 3.0.1-integridad-funcional.

## V3.0.0

- Dashboard rehecho como centro de control.
- Radar de oportunidades por prioridad.
- Métricas visibles de evaluación, preseñal, señal y alerta.
- Auditoría rápida con ciclos, rechazos, errores y duplicados.
- Ficha completa de cripto con predicción, MTF, Indicador Maestro y aprendizaje.
- Últimas evaluaciones por cripto con motivo de rechazo.
- Seguimiento de señales 15m/1h/4h/24h + MFE/MAE visible.
- Rendimiento global, por cripto y por timeframe.
- Diagnóstico ampliado.
- Prueba directa del canal Android de sonido/vibración.
- Mapa verificable de los 59 puntos.
- versionCode 3 / versionName 3.0.0-visible-59points.
# V3.8.0 — Resultado operativo y persistencia adaptada

- Separa el resultado operativo por primer toque de la persistencia medida en cierres.
- El aprendizaje y RENDIMIENTO usan exclusivamente resultados operativos V2 compatibles con el backtest.
- Conserva la clasificación anterior en `legacy_status` y reclasifica las señales exactas V3.7 desde Binance.
- Añade horizontes adaptados: 15m/30m/1h/4h/1d ya no comparten obligatoriamente la misma ventana.
- Amplía y pagina la descarga de velas de 5 minutos hasta cubrir señales diarias durante 14 días.
- Guarda objetivo, stop, umbral, motivo y momento de resolución de cada señal.
- Bloquea entradas tardías, operaciones sin espacio hasta el nivel contrario y contradicciones de intervalos superiores.
- BTC deja de otorgar puntos positivos por coincidencia y actúa como filtro de riesgo cuando contradice.
