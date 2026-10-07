# Investigación PR #9 — resultados parciales y reparación auditable

**No es un experimento completo.** La ejecución 37475805091 produjo 24 informes de 150 combinaciones. Los porcentajes siguientes corresponden al input anterior a la nueva reconciliación; no deben mezclarse con una repetición posterior.

## Inventario de la ejecución

Se consultaron las dos páginas: 152 trabajos (verificación, 150 combinaciones y resumen). Hubo 21 combinaciones con trabajo exitoso, 30 con fallo de datos y 99 canceladas. De estas últimas, 95 no llegaron a tener runner ni pasos y no tienen logs disponibles; cuatro sí comenzaron. Tres canceladas (TRX 15m, 30m y 1h) dejaron informe predictivo antes de cancelarse la preparación prospectiva auxiliar. Por ello existen 24 informes, aunque sólo 21 trabajos de combinación fueron exitosos.

El resumen rechazó correctamente declarar terminada la matriz: faltan 126 informes. No se ha demostrado un error aritmético de agregación; el fallo es la ausencia de resultados. Se añaden controles de duplicados, mezcla de versiones de motor o de inputs compartidos y listado explícito de no evaluables.

Las cancelaciones son estados de ejecución, no fallos predictivos ni insuficiencia de histórico. El flujo original usa concurrencia con cancelación de ejecuciones anteriores y también un paso que cancela repeticiones obsoletas. No se atribuye una avería de servidor ni un actor de cancelación que los logs no demuestran.

## Cobertura por moneda e intervalo

R = informe recuperado; D = fallo de discrepancia oficial; C = cancelada sin informe.

|Criptomoneda|15m|30m|1h|4h|1d|
|---|---|---|---|---|---|
|AAVE|C|C|C|C|C|
|ADA|D|D|D|D|D|
|ALGO|C|C|C|C|C|
|APT|C|C|C|C|C|
|ARB|C|C|C|C|C|
|ATOM|C|C|C|C|C|
|AVAX|D|D|D|D|D|
|BCH|C|C|C|C|C|
|BNB|R|R|R|R|R|
|BTC|R|R|R|R|R|
|DOGE|D|D|D|D|D|
|DOT|D|D|D|D|D|
|ETC|C|C|C|C|C|
|ETH|R|R|R|R|R|
|FIL|C|C|C|C|C|
|GRT|C|C|C|C|C|
|ICP|C|C|C|C|C|
|INJ|C|C|C|C|C|
|LINK|D|D|D|D|D|
|LTC|C|C|C|C|C|
|MKR|C|C|C|C|C|
|NEAR|C|C|C|C|C|
|OP|C|C|C|C|C|
|RUNE|C|C|C|C|C|
|SOL|D|D|D|D|D|
|SUI|C|C|C|C|C|
|TRX|R|R|R|R|C|
|UNI|C|C|C|C|C|
|VET|C|C|C|C|C|
|XRP|R|R|R|R|R|

## Resultados recuperados — 24 combinaciones

Precisión = aciertos / (aciertos + fallos). Neutras y pendientes se muestran aparte. La diferencia está en puntos porcentuales y compara conjuntos de señales diferentes; no es una comparación emparejada de operaciones. «Sin señales» significa modelo entrenado/calibrado que no superó el umbral, no modelo no evaluable.

|Moneda|Intervalo|Original señales|Aciertos/fallos|Neutras/pendientes|Precisión original|Alternativo señales|Aciertos/fallos|Neutras/pendientes|Precisión alternativo|Diferencia pp|
|---|---|---:|---|---|---:|---:|---|---|---:|---:|
|BNB|15m|6322|3128/3172|10/12|49.65%|0|0/0|0/0|Sin señales|—|
|BNB|1d|196|101/91|0/4|52.60%|1|1/0|0/0|100.00%|+47.40|
|BNB|1h|4884|2434/2437|3/10|49.97%|0|0/0|0/0|Sin señales|—|
|BNB|30m|5682|2839/2826|6/11|50.11%|0|0/0|0/0|Sin señales|—|
|BNB|4h|1464|698/761|0/5|47.84%|0|0/0|0/0|Sin señales|—|
|BTC|15m|6182|3078/3064|25/15|50.11%|0|0/0|0/0|Sin señales|—|
|BTC|1d|238|123/106|0/9|53.71%|5|5/0|0/0|100.00%|+46.29|
|BTC|1h|4811|2393/2393|16/9|50.00%|0|0/0|0/0|Sin señales|—|
|BTC|30m|5540|2802/2700|22/16|50.93%|0|0/0|0/0|Sin señales|—|
|BTC|4h|1498|755/737|1/5|50.60%|0|0/0|0/0|Sin señales|—|
|ETH|15m|6027|3046/2941|31/9|50.88%|0|0/0|0/0|Sin señales|—|
|ETH|1d|163|95/64|0/4|59.75%|1|1/0|0/0|100.00%|+40.25|
|ETH|1h|4583|2355/2207|10/11|51.62%|0|0/0|0/0|Sin señales|—|
|ETH|30m|5515|2831/2650|23/11|51.65%|0|0/0|0/0|Sin señales|—|
|ETH|4h|1366|692/671|0/3|50.77%|0|0/0|0/0|Sin señales|—|
|TRX|15m|6293|3413/2813|65/2|54.82%|151|52/95|4/0|35.37%|-19.44|
|TRX|1h|5015|2667/2294|54/0|53.76%|2387|868/1408|111/0|38.14%|-15.62|
|TRX|30m|5650|3042/2550|57/1|54.40%|726|264/411|51/0|39.11%|-15.29|
|TRX|4h|1566|843/720|2/1|53.93%|0|0/0|0/0|Sin señales|—|
|XRP|15m|6479|3119/3312|36/12|48.50%|0|0/0|0/0|Sin señales|—|
|XRP|1d|126|66/58|0/2|53.23%|11|5/6|0/0|45.45%|-7.77|
|XRP|1h|4983|2512/2460|5/6|50.52%|46|25/21|0/0|54.35%|+3.82|
|XRP|30m|5735|2885/2824|16/10|50.53%|53|44/9|0/0|83.02%|+32.48|
|XRP|4h|1390|737/649|2/2|53.17%|0|0/0|0/0|Sin señales|—|

## Conclusiones que sí respaldan estos informes

- Los 24 modelos alternativos recuperados eran evaluables. En 15 combinaciones no emitieron señales al umbral 0,65. Esto exige distinguir abstención y falta de entrenamiento.
- XRP 30m: alternativo 44 aciertos y 9 fallos (83,02%, 53 señales); original 2885 y 2824 (50,53%). Es una candidata exploratoria, seleccionada después de ver resultados parciales, sin validación nueva suficiente.
- TRX: el alternativo obtuvo 35,37% en 15m, 39,11% en 30m y 38,14% en 1h, frente a 54,82%, 54,40% y 53,76% del original. Estos resultados no apoyan una mejora general del alternativo.
- Los 100% de BTC, BNB y ETH diarios se basan en 5, 1 y 1 señales: no demuestran fiabilidad. XRP diario tuvo 5 aciertos y 6 fallos.
- No hay ganador probado de las 150 combinaciones, ni rentabilidad demostrada. Los horizontes se solapan, no se simulan costes/ejecución y el código fue diseñado después del inicio histórico.

## Reparación de datos

SOL: se ha reconciliado realmente el conflicto 2021-04-23 02:15 UTC: el daily 5m agregado y el daily 15m reproducen el native monthly 15m. Se sustituye sólo la vela 5m discrepante, guardando ambas versiones y hashes; se reconstruyen todos los intervalos desde el mismo input. La recuperación nativa posterior sigue comprobándose.

ADA: 2021-12-23 01:30 UTC presenta tres variantes. Monthly 5m agregado: OHLC 1,330/1,334/1,328/1,334 y volumen 510424,1. Monthly 15m: OHLC constante 1,330 y volumen 411,6. Daily 5m agregado y daily 15m: OHLC 1,330/1,335/1,328/1,332 y volumen 741294,9. La tercera variante está corroborada por dos representaciones oficiales; no es una convención vacía. Se preservan las dos anteriores. La revisión completa ADA sigue en curso.

La nueva herramienta rechaza desacuerdo entre daily 5m y daily native, falta de velas o propuestas incompatibles entre intervalos. No interpola ni rellena 5m ausentes. Cada input previo queda conservado localmente y recuperable en el artefacto original del run 37224269151; el registro incluye todas las filas cambiadas y SHA256 de archivos oficiales verificados con CHECKSUM.

## Verificación y límites

56 pruebas Python pasan, incluyendo agregación de una matriz sintética de 150 claves únicas, rechazo de duplicados, control cronológico y variables invariantes al cambiar datos futuros. Esa prueba sintética NO es el experimento histórico.

Los manifiestos recuperados de los 24 modelos respetan sus límites de entrenamiento y calibración y comparten hashes consistentes. El original filtra patrones por availableAt <= fecha simulada, noticias por timestamp <= fecha y contexto por velas cerradas. La paridad Kotlin y su prueba de perturbación futura se conservan como puerta obligatoria de CI; su nueva ejecución aún no está verificada.

Se leyó íntegramente el documento original de 59 puntos y la estructura/cobertura del repositorio. Cambios sólo en investigación, workflow de pruebas y documentación: no se modifica Android de producción, no se fusiona main y no se añaden horarios automáticos.

**Pendiente:** finalizar reparación real ADA/SOL, comprobación CI de paridad, repetición completa y revisión de cada combinación no evaluable. No se declara completado el trabajo.
