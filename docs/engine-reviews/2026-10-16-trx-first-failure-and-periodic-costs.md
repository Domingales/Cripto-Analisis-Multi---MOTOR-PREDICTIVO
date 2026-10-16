# Revisión del motor — 16 de octubre de 2026, 08:14 Madrid

No hay mejora predictiva independiente demostrada. El alternativo incorpora su primer FAIL de TRX4h. Se ha conectado la simulación de costes a las recogidas existentes sin modificar Android, modelos ni protocolos congelados.

## Registros actuales

| Cohorte | Decisiones | Señales | HIT | FAIL | NEUTRAL | Pendientes |
|---|---:|---:|---:|---:|---:|---:|
| XRP30m original | 31 | 4 | 2 | 1 | 0 | 1 |
| XRP30m alternativo | 31 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 291 | 45 | 10 | 10 | 2 | 23 |
| Seleccionadas15 alternativo | 291 | 8 | 4 | 1 | 0 | 3 |
| ADA1h main, cohorte distinta | 58 | 8 | 2 | 4 | 0 | 2 |

Cortes: seleccionadas15 2026-10-16T01:45:26.488Z; XRP 2026-10-16T01:30:39.032Z; ADA última ejecución 38845802279, success (iniciada 03:29:52Z). No comparar porcentajes de estas cohortes como si fueran muestras emparejadas.

Respecto al cierre del 15 a las 08:40 Madrid: seleccionadas15 +48 decisiones, original +3 señales, +3 HIT y +1 FAIL; alternativo sin nuevas señales, +1 FAIL. XRP +5 decisiones, sin resultados nuevos aceptados. ADA +4 decisiones y una nueva señal pendiente.

## TRX y comparaciones emparejadas

TRX4h alternativo: 3 HIT, 1 FAIL, 3 pendientes. TRX1h alternativo: 1 HIT. Estas señales forman el mismo único grupo multitemporal transitivo cuya primera señal continúa HIT. Los cuatro HIT anteriores compartieron una vela 5m y no se convierten en cuatro pruebas independientes por sumar un resultado nuevo.

El FAIL es TRX-4h-1791835199999: compra registrada 12/10 23:16:25.936Z, ventana hasta 15/10 23:19:59.999Z, STOP_FIRST. La venta original aceptada en esa misma decisión fue HIT. MFE de toda la ventana 1,608986% frente a MAE -0,728597% no revierte el stop previo ni equivale a beneficio realizable.

| Decisión aceptada por ambos | Original | Alternativo |
|---|---|---|
| TRX-4h-1791734399999 | FAIL | HIT |
| TRX-4h-1791835199999 | HIT | FAIL |
| TRX-4h-1791907199999 | PENDIENTE | PENDIENTE |

Dos casos resueltos, uno favorable a cada motor, dependientes y dentro del mismo grupo de ventanas. Sin superioridad verificable.

Hay una ventana TRX4h ya vencida a las 05:59:59.999 Madrid y aún pendiente en el último corte de recogida, anterior a su vencimiento. Las otras terminan hoy a las 14:14:59.999 y 21:39:59.999 Madrid. No resolver desde precios inventados ni sin la ventana oficial completa. ADA conserva también una ventana que venció a las 05:39:59.999 Madrid, después del corte de su última recogida; la otra vence hoy a las 10:59:59.999.

## Calibración y dependencia

XRP alternativo: cero señales aceptadas, modelo evaluable; 25 puntuaciones resueltas, Brier 0,245768. TRX alternativo: Brier 15m 0,270564; 30m 0,270207; 1h 0,271592 (17 puntuaciones cada uno); 4h 0,201775 (12). No son casos independientes ni señales operables en todos los casos. El Brier 4h empeora desde 0,126334 en 9 puntuaciones del informe anterior; todavía no demuestra calibración estable ni ventaja.

Agrupación adicional por activo/motor incluyendo plazos: original seis grupos, primeras señales 2 HIT, 1 NEUTRAL y 3 pendientes; alternativo un grupo, primera señal HIT. Ventanas no solapadas tampoco prueban independencia estadística. No se ajustaron umbrales ni se eligieron casos tras conocer resultados.

## Prueba de ejecución y costes: inicio real sin operaciones

Reserva fija desde 2026-10-16T00:00Z, conservada. Entran 15 decisiones seleccionadas y 1 XRP. Ninguno de los dos motores aceptó señales en esas 16 decisiones: cero operaciones admitidas, cero retornos calculables y cero pares de ejecución resueltos. Esto NO es una precisión o rentabilidad del 0% ni un conjunto de operaciones pendientes.

Se evaluaron los diarios actuales con verify_frozen y evaluate. Los snapshots execution-costs.json se identifican como JOURNAL_ONLY_NO_ACCEPTED_SIGNALS_NO_MARKET_PRICE_USED: no fue necesario ni se afirmó usar precios oficiales de ejecución porque no hay operaciones. Se guardan hashes de diarios, recibos y freeze-lock. No se simularon retrospectivamente los aciertos TRX anteriores al inicio.

Commit de investigación [6cdc957](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/commit/6cdc957ef705908bae525860bd8e52c61f793cb5):
- Los lanzadores seleccionadas15 y XRP llaman al cálculo después de registrar y confirmar durablemente las decisiones; reutilizan las velas nativas que ya descargan, sin duplicar capturas ni reentrenar.
- Cada resultado derivado se conserva en su propia carpeta de cohorte usando el mismo push fast-forward conservador, sin force ni reconciliar escritores ajenos.
- La herramienta admite múltiples diarios mensuales y registra hashes de todos los diarios, recibos, manifiesto y freeze-lock, fecha de evaluación y corte de mercado.
- No cambia etiquetas HIT/FAIL/NEUTRAL, selección, costes supuestos ni entrada/horizonte registrados. Si una ventana no ha madurado o faltan velas, no inventa resultados.
- La conexión periódica está publicada y sus comprobaciones pasan; su primera ejecución completa dentro de una nueva recogida aún debe verificarse. No declarar continuidad de ese nuevo postprocesado antes de observarla.

## Integridad y comprobaciones

Se volvió a leer íntegro el documento original de 59 puntos antes de modificar código. La implementación sólo toca herramientas de investigación; preserva motor Android, interfaz, alarmas, persistencia y vigilancia híbrida.

Auditoría local reproducible desde fuentes recién recuperadas de GitHub: 291 decisiones seleccionadas y 31 XRP, todas únicas; cada evento coincide con el SHA256 de su recibo, registrado antes de first_open. Coinciden protocolo, motor y modelos con sus locks. No se usó scratch como fuente de estado ni se repitió matriz150.

- Seleccionadas15 [38836013932](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/38836013932): success; recuperó 276 y añadió 15, total 291. 85 pruebas Python anteriores al cambio, compilación y testDebugUnitTest Kotlin correctos.
- XRP [38835265183](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/38835265183): success; recuperó 30 y añadió 1, total 31; 85 pruebas Python y compilación/pruebas Kotlin correctas.
- Nueva verificación [38859902501](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/38859902501): success; 87 pruebas Python (17 de costes) y bash -n en ambos lanzadores.
- Artefacto seleccionado 12019207404, sha256:fc4fa01dd7e9188d172226ec63b1bc9a41afc083dc8d045b29e4411927768fa4. XRP 12019645105, sha256:07a792fbe90b38ebc19efe2ed8f460553d05f5064d41f7330170919111a6b39c. Vigentes; los diarios y snapshots se conservan además en GitHub.
- Diarios fuente: blob seleccionado 39ee3f562c505d12221c1f29a596503197a24e6e; XRP c67e39191ec6275a957b5cd232a98e7c27792b83.
- Antes de publicar se comprobó que no había ejecuciones en curso ni en cola. No se repitió la colisión de ayer ni se forzaron refs.
- PR9 abierta y no fusionable: no se fusionó, ni hace falta hacerlo para estas herramientas. La configuración horaria no garantiza una ejecución por hora; siguen existiendo separaciones de varias horas.

## Próximo paso

Verificar en la próxima recogida el nuevo execution-costs.json durable, su recuperación y sus hashes; resolver las ventanas vencidas con datos completos. Acumular señales posteriores a la reserva para evaluar costes. Ampliar después informes por régimen y bloques temporales sin optimizar con resultados ya vistos ni aplicar cambios a producción sin ventaja independiente. No hay bloqueo total ni rentabilidad demostrada.
