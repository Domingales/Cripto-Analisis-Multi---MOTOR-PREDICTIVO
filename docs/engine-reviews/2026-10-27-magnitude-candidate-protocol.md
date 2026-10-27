# Candidato de magnitud/plazo V1 — protocolo 27/10/2026
Commit experimental:1f51814710499bab4c364ea592d0bb35d34e26cb.
Actions40580602201. Documento registrado al comienzo de ejecución, antes de consultar resultados.

## Hipótesis
Clasificador binario por dirección para HIT exacto en24h, usando objetivo común igual al punto medio del rango REAL emitido por el motor Kotlin original en cada fecha histórica. No usar el targetPct ATR del antiguo primer toque: es otra pregunta.

XRP30m, contextos15m/30m/4h/1d y BTC4h. Velas oficiales recuperadas de37224269151 con manifests/checksums; reconciliación y reparación nativa existentes, sin relajar discrepancias ni rellenar precios. Entradas al cierre conocido; primera vela5m posterior. Noticias ausentes neutrales.
Se reconstruye únicamente esta combinación para guardar los rangos que faltaban en el archivo antiguo; no repetir matriz150.

## Evaluación idéntica a Android
ForecastCriterion.kt sin cambios etiqueta BUY/SELL: HIT24h, APPROXIMATE75%48h, LATE hasta120h y FAIL después; huecos pendientes. Nuevos contratos exclusivamente HISTÓRICOS, no retroactivos en registros prospectivos.

## Modelo preespecificado
45 variables as-of existentes + magnitud objetivo común. GradientBoostingClassifier40árboles, profundidad2, learning_rate0,05, random_state17; calibración logística C1. Dos modelos por BUY/SELL, clase positiva sólo HIT exacto. Elegir mayor puntuación; aceptar >=0,90, sin búsqueda de umbral según examen. Puntuación no certificada.
TRAIN:etiquetas completas120h estrictamente antes de2023-04-01.
CALIBRATION:orígenes desde2023-04-01 con etiquetas completas antes de2023-07-01.
Bloque julio–septiembre queda fuera del ajuste de esta versión.
EXAMINATION:desde2023-10-02 hasta2026-10-02T15Z; fronteras purgadas120h. Modelo/manifest guardados antes de predicciones; predicciones guardadas antes de cargar etiquetas del examen.

## Comparación y límites
Original aceptado vs candidato en mismas oportunidades de XRP30m, mismo precio/rango/plazos; direcciones pueden diferir. Informar todas las decisiones, cobertura, HIT/APPROXIMATE/LATE/FAIL/PENDING y Brier de ambas direcciones, no sólo aceptadas. Agrupación transitiva120h y primera señal sin resultados; no afirmar independencia automática.
El examen2023–2026 ya fue inspeccionado para otras investigaciones: este resultado es EXPLORATORIO, nunca validación independiente ni certificación90%.
No se modela ejecución/costes en esta primera fase; alcanzar magnitud no implica operación rentable. No se amplían rangos para inflar cobertura.
Si obtiene cero señales, decir cero señales y precisión indefinida. Si obtiene90% observado, no convertirlo en90% real garantizado.
No modificar producción, candados, umbrales/modelos de cohortes activas ni Android funcional. Hash predictor esperado:f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7.
No promover hasta contraste futuro registrado y evaluación de riesgos/costes. Siguiente paso depende del resultado, sin afinar sobre el examen y volverlo a llamar independiente.
