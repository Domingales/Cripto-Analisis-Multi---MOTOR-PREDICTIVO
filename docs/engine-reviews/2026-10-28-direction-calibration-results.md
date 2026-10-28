# Diagnóstico de calibración por dirección — 28/10/2026

## Resultado

Se diagnosticó el candidato congelado XRP30m MAGNITUDE_TIME_V1 sin reentrenar, cambiar umbrales ni tocar producción. La calibración ajustada en abril-junio de 2023 no conserva su calidad en el periodo cronológico posterior. No se ha demostrado un 90% fiable.

| Desarrollo posterior (4224 ventanas maduras por dirección) | BUY | SELL |
|---|---:|---:|
| Probabilidad media sin calibrar | 69,86% | 81,72% |
| Probabilidad media calibrada | 76,39% | 75,26% |
| HIT exacto observado | 70,03% | 81,08% |
| Sesgo calibrado, puntos porcentuales | +6,37 | -5,82 |
| Brier sin calibrar (menor es mejor) | 0,208755 | 0,153302 |
| Brier calibrado | 0,212365 | 0,156191 |
| Brier constante aprendido en TRAIN | 0,213268 | 0,154077 |
| AUC | 0,56030 | 0,55005 |

La calibración empeora Brier en ambas direcciones; SELL calibrado incluso queda peor que la constante TRAIN. La discriminación también es débil: corregir porcentajes no basta para seleccionar buenos pronósticos. Estos hallazgos son compatibles con deriva y sobreajuste; no prueban una única causa.

## Método y límites

- Protocolo publicado antes del análisis: docs/engine-reviews/2026-10-28-direction-calibration-protocol.md.
- TRAIN: etiquetas con horizonte completo de 120h maduro antes de abril de 2023.
- CALIBRATION: abril-junio de 2023, maduro antes de julio; muestra usada por el calibrador, no validación independiente.
- DEVELOPMENT_CHECK: julio-septiembre de 2023, horizonte de 120h maduro estrictamente antes del 02/10/2023.
- Cero etiquetas del examen utilizadas; embargo excluido; cero ajustes realizados. El desarrollo ahora inspeccionado no puede reutilizarse como reserva independiente.
- Todas las puntuaciones BUY y SELL; pendientes no convertidos en fallos. Bins fijos, perfiles mensuales, régimen y magnitud; logloss, Brier, AUC, fiabilidad y cobertura.
- Solo siete bloques temporales de 14 días en calibración y desarrollo: el mínimo predefinido de doce no se alcanza, por lo que no se produce un intervalo bootstrap engañoso.
- Las ventanas se solapan. No son 4224 observaciones independientes.
- El evento es alcanzar el objetivo completo en 24h. No equivale a rentabilidad ni al resultado operativo V2.
- BUY y SELL no son complementarios: en 2179 de las 4224 ventanas ambos objetivos se alcanzaron; en 20, ninguno. No imponer que sus probabilidades sumen uno.
- Alcance: este candidato XRP30m, no todos los activos ni la confianza heurística del predictor original.

## Hallazgos secundarios, no reglas nuevas

Para BUY y objetivo >=4%, solo 53 ventanas de desarrollo: 39,62% HIT, sobreestimación de 32,06 puntos. La fase de calibración no tenía casos en ese rango. Hay falta de soporte; no adoptar una corrección a partir de esa muestra pequeña y dependiente.

El umbral diagnóstico fijo SELL >=0,8 produce 59 HIT, 1 APPROXIMATE y 2 LATE en 62 ventanas (95,16% exacto). Pero se concentran en solo tres episodios transitivos de solapamiento de 120h, con primera señal HIT en los tres. No demuestra 90% independiente ni justifica seleccionar ese umbral después de ver resultados. Con >=0,9 no hay señales en desarrollo en ninguna dirección.

## Verificación y procedencia

- Commit del diagnóstico: c4a2ae9385a36c4bb379ed97c2b5adc9844896e0.
- Actions: https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/40667356805 — SUCCESS, 131 pruebas Python y Android testDebugUnitTest assembleDebug correctos.
- Artefacto de salida 12805869331: report.json, summary.json y 69592 puntuaciones pre-examen. SHA256 ZIP: fa10f69b52c137c3e2632f050319e990f1d2e87ac244610ca4cdd43f0f4d56db.
- Fuente original run 40580602201 / artefacto 12768818184. SHA256 ZIP: 4738051e6fd0538d1c45b3c0a409c55d03f9c62ff3ffd355a7540bfc303e0184.
- Modelo SHA256: 781f093ff8b87cb733fab9dfe906b124772ccd7be2afcef5ba09b21782d2a10e.
- Predictor original intacto: f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7.
- Se usó la versión original scikit-learn 1.7.2, sin deserializar con una versión diferente.

## Bloqueo de archivo y siguiente paso

La revisión automática rechazó la publicación de un payload binario/base64 de evidencia por no poder verificar contenido y sensibilidad. No se ha eludido: no se afirma que el modelo ni las puntuaciones completas estén archivados durablemente en la rama. Permanecen en el artefacto verificado de Actions, sujeto a su caducidad. Este informe legible conserva resultados, límites y hashes.

Siguiente experimento propuesto: preregistrar comparadores de calibración por dirección con validaciones cronológicas, embargo y soporte por magnitud/régimen, además de medir si mejora la discriminación. Elegir exclusivamente en desarrollo y validar en una reserva realmente no utilizada o señales futuras. No cambiar el calibrador congelado, no activar SELL >=0,8 y no volver a ajustar con el examen ya inspeccionado.
