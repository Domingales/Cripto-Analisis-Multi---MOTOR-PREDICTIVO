# XRP30m: recogida periódica y continuidad comprobadas

Revisión: 9 de octubre de 2026, 09:08 Europe/Madrid.

Se ha activado la recogida horaria de investigación. Sólo el lanzador `.github/workflows/xrp-collection.yml` se ha publicado en main: hace checkout de research/expanded-replay-150. No se ha fusionado la PR #9 ni cambiado Android, sus alarmas, su apariencia o el motor de producción. Se ha leído primero el documento original de 59 requisitos (libfile_e43bbafca5108191a8290427f667cef0).

## Evidencia de continuidad

| Ejecución | Estado | Decisiones recuperadas | Nuevas | Total |
|---|---|---:|---:|---:|
| 37896656268 | success | 0 | 1 | 1 |
| 37896847589 | success | 1 | 0 | 1 |

La segunda ejecución recuperó el registro desde git, verificó el mismo modelo y protocolo y deduplicó la misma vela. Ambas ejecuciones descargaron datos oficiales cerrados y calcularon la decisión Kotlin. La frecuencia es horaria y puede sufrir retrasos de GitHub; no es vigilancia continua. No se reconstruyen retrospectivamente las velas omitidas desde el inicio declarado a las 00:00Z.

Modelo conservado desde el artefacto 11549284298 de la preparación 37775349894; 78.828 train y 8.805 calibration por dirección. SHA256 del modelo: 009e602aef903ce3ff3b6ce8000f07ad5c38fbd6c0d4839d14167f8acfaa21d3. No se reentrena en cada recogida. El modelo, manifiesto, bloqueo, journal mensual, recibos y resúmenes están en evaluation/prospective-xrp, versionados en la rama de investigación, sin depender de la caducidad de artefactos.

Primera decisión XRP-30m-1791529199999: escrita a las 07:03:44.437Z; confirmada remotamente a las 07:03:45.411Z; primera vela de resultado a las 07:05Z. Precio conocido 1,4004 USDT. Original WAIT, sin señal; alternativo SELL con puntuación calibrada 0,515499425, por debajo del umbral congelado 0,65, sin señal. Esa puntuación no acredita fiabilidad futura.

Ambos motores: 1 decisión, 0 señales, 0 HIT, 0 FAIL, 0 NEUTRAL y 0 señales pendientes. Hay una puntuación alternativa todavía sin resultado de su ventana de 24h; se conserva para Brier/calibración aunque no supere el umbral. No hay precisión calculable ni ventaja independiente demostrada.

La escritura remota del pronóstico se confirma antes de la primera vela 5m de resultado. Una escritura tardía, un recibo ausente/modificado, cambio de modelo/protocolo/engine o un push rechazado detienen la recogida, sin forzar refs ni sustituir forecasts previos. Los episodios siguen el protocolo original y se elige su primera señal sin mirar el resultado.

## Validación y otras cohortes

68 pruebas Python locales y en la recogida; Android 37896638678 success. Ambas recogidas y las pruebas Kotlin actuales success. No se ha repetido la matriz de 150 combinaciones.

ADA main: fuente actual evaluation/prospective/events.jsonl en d645b65a856a90a081ac2b6a8ed087f70bd3b296: 33 decisiones, 2 señales, 2 HIT, 0 FAIL, 0 NEUTRAL y 0 señales pendientes. Muestra insuficiente.

El protocolo de 15 combinaciones permanece byte-identical y sin lanzador activado en main; sus ejecuciones de PR son pruebas, no un registro prospectivo durable activado. No se declara activo ese seguimiento.

La actualización de investigación disparó también la preparación antigua XRP 37896638626. Su modelo y registro de prueba NO reemplazan la cohorte ni el modelo congelado. Se elimina el disparador pull_request de esa preparación para evitar nuevos entrenamientos innecesarios: queda manual. La recogida usa exclusivamente el modelo original comprobado.

Siguiente trabajo: revisar las siguientes recogidas y resolver las primeras puntuaciones sólo al vencer su ventana; preparar activación separada y durable de las 15 combinaciones sin alterar su protocolo. Más adelante, hipótesis limitadas y validación independiente por régimen y costes; ningún filtro irá a producción sin evidencia independiente.
