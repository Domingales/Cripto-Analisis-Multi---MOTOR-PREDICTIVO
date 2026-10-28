# Resultados del experimento diario de volumen — 28/10/2026
Run40649782565 success (tres trabajos). Código3d4b09bf53ce2027fc21d65bf5e4d30517945954.119tests Python en cada trabajo, controles Kotlin de paridad/no-futuro, mismo ForecastCriterion.kt y testDebugUnitTest/assembleDebug correctos. Tres APK de prueba, sin predictor productivo nuevo.

## Alcance
3.288 oportunidades:1.096por activo, BTC/ETH/XRP1dUTC, examen2023-10-02–2026-10-02T15Z. Preparación histórica desde2020 con fuentes oficiales/checksums. No repetir matriz150.
OriginalBUY y reglasBUY usan mismo precio/rango/plazos, pero seleccionan fechas distintas; tasas condicionales descriptivas, no pares independientes. BUY_ALL queda como control en reportes. UD50 y firmaPP10 definidas previamente en2026-10-28-volume-daily-protocol.md.

|Activo|OriginalBUY exacto|UD50 exacto|PP10_VOLUME exacto|
|---|---:|---:|---:|
|BTC|20/150=13,33% (1pendiente)|54/568=9,51% (1pendiente)|11/93=11,83%|
|ETH|15/104=14,42%|49/571=8,58% (4pendientes)|14/100=14,00%|
|XRP|14/58=24,14%|52/550=9,45% (5pendientes)|22/114=19,30%|

Son HIT de objetivo completo en24h, NO porcentajes de dirección correcta. Aproximados75%48h, tardíos120h, fallos y pendientes están separados en cada report.json.
Objetivos originales diarios:BTC2,04%–10,57%;ETH2,53%–15,37%;XRP2,97%–20,32%. Más exigentes que los del ensayo anterior XRP30m; NO comparar estas tasas con86,36% exactos+aproximados de otro plazo.

## Costes y selección
UD50 sin original:media hipotética por operación admitida BTC+0,238% (123casos),ETH consultarreport,XRP−0,692% (121casos).
PP10 sin original:BTC−0,530% (55casos),XRP−1,024% (60casos);ETH consultarreport.
Son medias por caso con nominal fijo, NO rentabilidad de cartera ni evidencia independiente. Fee0,10%/lado y slip0,10%/lado principales; las otras sensibilidades y todos los casos están guardados. Stop simétrico experimental, no invalidación original.

Filtros secundarios sobre originalBUY elevan algunas tasas al seleccionar menos fechas, pero no prueban ventaja económica estable:ejemplo XRP+PP10 pasa a11/37=29,73%exactos, mientras media neta simulada empeora de−0,794% originalBUY (28admisiones) a−1,600% (23admisiones).
Ejemplo BTC originalBUY+PP10 da media positiva+0,691% en34admisiones; pequeño, seleccionado entre múltiples contrastes y dependiente. No declarar ganador ni activar por este máximo.

## Conclusión
Las firmas de volumen aisladas NO superan al originalBUY en tasa exacta en ninguno de los tres activos. No se demuestra ventaja independiente ni90%; no se despliega ningún filtro. Sólo se probó la firma de volumen del pocket pivot, no su método completo con estructura/medias/fundamentales.
Seis contrastes principales y filtros secundarios:sin inferir significación desde máximos, sin intervalos binomiales de señales solapadas. Antes de cualquier promoción faltan incertidumbre temporal y confirmación futura registrada. No afinar umbrales con este examen y volverlo a llamar independiente.
Cero actores identificados:UD50 no es entrada neta de capital ni detección institucional.

## Evidencia persistente
En docs/engine-reviews/volume-daily-2026-10-28/{BTC,ETH,XRP}/:
- report.json legible.
- evidence.tar.gz.base64 conserva manifest, contratos históricos, todas las decisiones previas a etiquetas, labelsKotlin y todos los casos de costes. Decodificar base64 y abrir tar.gz; no contiene recibos prospectivos ficticios.
Se verificó igualdad byte a byte mediante hashes Git de las tres copias remotas frente a originales locales.
Artefactos12798957982BTC,12799123137ETH,12798738784XRP; ZIP contrastados con digest oficial SHA256:
BTC5bd3b0b1d7574db265b8b4c68b3b9c45f50d9d37a12e614e7325493b671760a0
ETHa476242c1e0361a429829c97b52c46a474afcdb337233de76d47f4ede323fde5
XRP46604edb05a22bfbb14be8bcfa00ae5bbd90f4db4f4fb1214457aceb6f1f84c8

Producción, diseño, alarmas, persistencia, vigilancia híbrida y modelos prospectivos congelados intactos. Hash predictor originalf8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7.
