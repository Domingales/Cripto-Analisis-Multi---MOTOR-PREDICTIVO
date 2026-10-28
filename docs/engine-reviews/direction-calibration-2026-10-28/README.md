# Evidencia durable del diagnóstico BUY/SELL — 28/10/2026

Publicación autorizada explícitamente por José Domingo después del bloqueo inicial de revisión automática. Archivo de investigación: no modifica producción ni activa el candidato.

Origen: run 40667356805, commit c4a2ae9385a36c4bb379ed97c2b5adc9844896e0, artefacto 12805869331.
ZIP original SHA256: fa10f69b52c137c3e2632f050319e990f1d2e87ac244610ca4cdd43f0f4d56db.
131 pruebas Python y testDebugUnitTest assembleDebug correctos. No refit ni etiquetas de examen.

## Contenido

- candidate.pkl: candidato congelado de investigación (no ejecutar pickle de fuentes no confiables).
- candidate-freeze.json: contrato original de congelación.
- report.json: diagnóstico completo.
- summary.json: resumen de resultados.
- diagnostic-scores-pre-exam.jsonl.gz.part-aa, ab, ac, ad: cuatro fragmentos binarios de un único gzip. Contiene 69592 filas, únicamente periodos permitidos anteriores al examen; no son observaciones independientes.

Los JSON preservan el contenido original; únicamente se omite el salto de línea final. Los binarios se verificaron byte a byte mediante identidad de blob Git antes de crear el commit.

## Reconstrucción y SHA256

Concatenar los cuatro fragmentos en el orden aa, ab, ac, ad, y después descomprimir gzip.

SHA256 del gzip reconstruido:
ad3700f3881f05e13c539789aab4ff08296ef0391c1419ac82486f546dddafa7

SHA256 por fragmento:
- aa: 558c92349aa84aa3c83a0187b225e833d1ccf959934fbc93be6265d6d8bb9b37
- ab: f3590afa706dc72acd4d1ca97389828848c7d0003996a537cb2165207f58cdff
- ac: 9e2f1cbcde440530bf940842746ae6b17fd61295691e9450f094f33dfd83f778
- ad: 2b339a2984dfc8df172ae600c99c704caa535701c513cb443726af90fa852caf

SHA256 candidate.pkl:
781f093ff8b87cb733fab9dfe906b124772ccd7be2afcef5ba09b21782d2a10e

## Interpretación

La calibración empeora Brier en ambas direcciones en desarrollo cronológico. BUY: media calibrada76,39% frente70,03% exactos; SELL75,26% frente81,08%. El95,16% de62 ventanas SELL>=0,8 corresponde a solo3 episodios transitivos solapados: no demuestra90% independiente. Modelos, umbrales y predictor original intactos.

Esta publicación resuelve el bloqueo de archivo indicado en el informe anterior; no aporta una mejora predictiva nueva. Las pruebas no se repiten porque sólo se archiva evidencia ya comprobada.
