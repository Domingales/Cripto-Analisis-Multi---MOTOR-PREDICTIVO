# CriptoAnálisis Multi 3.8.0 — Resultados predictivos V2

Proyecto Android Studio/Kotlin para vigilancia técnica de múltiples criptomonedas.

## Diferencias visibles frente a V1/V2

- Dashboard completamente rehecho: estado del motor, métricas, auditoría rápida y radar de oportunidades.
- Ficha por cripto con recomendación, predicción, técnico, MTF, Indicador Maestro desglosado, aprendizaje, últimas evaluaciones y seguimiento de señales.
- Oportunidades separadas en alta prioridad, preseñal/cerca del umbral y no operar.
- Rendimiento total, por cripto, timeframe, dirección BUY/SELL, régimen y riesgo.
- Diagnóstico con contadores trazables de evaluación → preseñal → señal → alerta.
- Configuración global con prueba real del canal de sonido/vibración.
- Pantalla `Mapa de los 59 puntos` para localizar cada capacidad en la APK.
- Backtest, exportación CSV/TXT, configuración individual por cripto y recuperación tras reinicio.
- Exportación individual de la ficha completa mediante portapapeles, compartir, TXT, CSV compatible con Excel y RTF para Word.
- Probabilidad histórica solo desde 10 resultados comparables resueltos.
- Intravela estricta: una vela abierta solo puede producir PRESEÑAL.
- Backtest MTF alineado temporalmente y sin usar velas futuras.
- Vigilancia permanente mediante ForegroundService `specialUse`, programación local con el intervalo configurado, JobScheduler de respaldo y activación híbrida Cloudflare Workers Free + Firebase Spark.
- Alta de señal transaccional con snapshot técnico completo, seguimientos y outbox recuperable de notificaciones.
- Aprendizaje comparable mediante RSI, ADX, ATR, EMA, volumen, estructura, S/R, MTF y contexto.
- Caché SQLite de velas, EMA/RSI incrementales en velas consecutivas e indicadores reutilizables por cierre de vela.
- Exportación global completa a portapapeles, compartir, TXT, CSV/Excel y RTF/Word; backtest separado por BUY/SELL.
- Catálogo Spot USDT de Binance con búsqueda, selección múltiple y favoritos, sin un límite artificial de monedas.
- Preferencias independientes de sonido y vibración por criptomoneda, heredando la configuración global.
- Alarmas de precio por encima/debajo, duración sostenida `D:HH:MM`, observación y estados auditables.
- Dashboard ordenable por confianza, variación, nombre, señal o última alerta; seguimiento activo destacado en morado.
- Ficha con gráfico, soportes y resistencias, EMA20/50/200, DMI, ATR y expansión de volumen.
- Menú hamburguesa global configurable a izquierda/derecha y diagnóstico de red, API, motor y cada activo.
- Régimen `MIXED` neutral: un mercado ambiguo ya no se etiqueta automáticamente como posible giro.
- En modo intravela se analiza a la vez la vela abierta (PRESEÑAL) y la última cerrada (posible SIGNAL).
- Snapshot V6 reproducible con indicadores, rupturas, score, aprendizaje, noticias y configuración original.
- Casos comparables recalculados con el lado BUY/SELL definitivo.
- Resultado operativo por primer toque con velas históricas de 5 minutos y persistencia independiente con horizontes adaptados al timeframe.
- Seguimiento anclado al cierre que fijó la entrada; ninguna vela iniciada antes de la señal puede contaminar MFE/MAE.
- Aprendizaje y rendimiento separados de la persistencia y de resultados heredados; sólo `EXACT_FIRST_TOUCH_5M_V2` alimenta la calibración.
- Exportación global por flujo y en segundo plano, apta para historiales grandes; compartir usa un archivo temporal seguro mediante FileProvider.
- Backtest walk-forward con MTF, contexto BTC histórico, aprendizaje disponible en cada fecha, ventana adaptada, primer toque y cooldown.
- Filtros contra entrada tardía, falta de espacio hasta soporte/resistencia, giro inmediato del impulso y contradicción de intervalos superiores.
- BTC confirma sin sumar puntos artificiales; únicamente penaliza cuando contradice la operación o aumenta el riesgo.
- Detector extremo completamente paralelo: `POSIBLE EXPLOSIÓN ALCISTA/BAJISTA` sólo con volumen ≥2x, ruptura cerrada, ATR ≥1,10x y creciendo, ADX ≥25 y creciendo con DMI coherente, y MTF ≥3 intervalos sin contradicción.
- Tablas, UID, outbox, contadores, seguimiento ±1 ATR, historial, notificaciones, ajustes y exportación propios; no modifica SignalEngine, score, confianza, probabilidad, umbral ni aprendizaje de señales.
- Los once accesos del antiguo bloque inferior `Centro de control` están integrados en el menú hamburguesa superior. El bloque inferior se elimina y el menú queda inicialmente situado a la derecha, conservando su ajuste posterior izquierda/derecha.
- El botón hamburguesa usa un icono real de tres líneas doradas, aproximadamente el doble de grande. El desplegable es desplazable y todas sus opciones tienen fondo negro, marco dorado y texto dorado.
- Las notificaciones de señales, alarmas de precio y posibles explosiones muestran y conservan la hora real del evento de mercado, aunque Android las publique o recupere más tarde.
- Un bloqueo persistente coordina los ciclos local, remoto, manual y de respaldo para impedir análisis duplicados al despertar el teléfono.
- Diagnóstico ampliado con latido del servicio, origen del último ciclo, retraso FCM y estado de optimización de batería.

## Motor

Fuente pública Binance Spot USDT. Indicadores: EMA20/50/200, RSI14, ATR, DMI/ADX, volumen, soportes/resistencias, estructura, régimen, MTF, contexto BTC, histórico comparable y contexto auxiliar de noticias.

Una puntuación alta no equivale automáticamente a alerta. El motor persiste el análisis y sólo crea una alerta después de insertar una señal única que supera filtros y deduplicación/cooldown.

## Android Studio

- `applicationId`: `com.domingales.criptoanalisis.multi`
- `minSdk`: 26
- `targetSdk`: 35
- `versionCode`: 20
- `versionName`: `3.8.0-resultados-predictivos-v2`
- JDK 17

Abrir la carpeta `PROYECTO CriptoAnalisisMulti` en Android Studio y ejecutar `Build > Assemble Project` antes de instalar en el teléfono.

La conexión híbrida requiere configuración externa y credenciales propias. Consulta `GUIA_HIBRIDA_GRATUITA.md`; la app compila y funciona en modo local sin `google-services.json`, aunque para máxima continuidad con pantalla apagada se recomienda conservar Firebase y excluir la app del ahorro agresivo de batería del fabricante.
