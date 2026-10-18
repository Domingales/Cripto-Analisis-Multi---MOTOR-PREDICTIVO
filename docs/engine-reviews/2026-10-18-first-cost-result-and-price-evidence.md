# Investigación y ejecución — 18 de octubre de 2026, 08:28 Madrid

Primer retorno simulado resuelto: una compra del motor original en TRX30m produce +0,078902% en el escenario principal fijado, pero -0,121256% con más deslizamiento. Un único caso no demuestra rentabilidad ni mejora predictiva. El otro caso inicial, RUNE15m, figura no ejecutable a los niveles registrados, aunque su etiqueta direccional sea HIT.

## Seguimiento direccional actual

| Cohorte | Decisiones | Señales | HIT | FAIL | NEUTRAL | Pendientes |
|---|---:|---:|---:|---:|---:|---:|
| XRP30m original | 44 | 5 | 3 | 1 | 0 | 1 |
| XRP30m alternativo | 44 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 423 | 62 | 20 | 23 | 2 | 17 |
| Seleccionadas15 alternativo | 423 | 8 | 4 | 4 | 0 | 0 |
| ADA1h main, cohorte distinta | 67 | 11 | 4 | 5 | 0 | 2 |

Cortes: seleccionadas15 2026-10-18T05:10:02.178Z; XRP 2026-10-18T06:03:25.964Z. ADA recuperada de main tras la ejecución 39093552775, iniciada 03:25:12Z y terminada success.

Desde el informe del 17: seleccionadas15 +66 decisiones y +10 señales originales; original +7 HIT/+2 FAIL, alternativo sin señales ni resultados nuevos. XRP +7 decisiones y una nueva señal pendiente. ADA +5 decisiones, +2 señales y +1 FAIL. Las cohortes no constituyen una comparación emparejada de porcentajes.

Las ocho señales alternativas TRX siguen en un único grupo multitemporal: 4 HIT/4 FAIL, primera señal HIT. En 4h son 3 HIT/4 FAIL; 1h 1 HIT. Los tres casos aceptados por ambos motores favorecen dos veces al original y una al alternativo, sin independencia estadística suficiente para declarar superioridad. No se modifica el umbral del 65% ni se reentrena.

## Primer caso con costes: original TRX30m

Decisión TRX-30m-1792198799999, BUY. Referencia registrada 0,3356 USDT, objetivo congelado 0,45%; apertura simulada posterior al registro 0,3355 y salida TARGET a 0,3371102. Se esperó el horizonte completo. El caso pertenece al original, no al alternativo.

| Comisión por lado | Deslizamiento por lado | Retorno nocional simulado |
|---|---|---:|
| 0,10% | 0% | +0,279460% |
| 0,10% | 0,10% — principal | +0,078902% |
| 0,10% | 0,20% | -0,121256% |

Son supuestos registrados de sensibilidad, no tarifas verificadas de eToro ni beneficio ejecutado. Se aplican deslizamiento adverso y comisión sobre cada importe negociado. No escoger el escenario favorable ni extrapolar este único resultado a cartera, rentabilidad anual o ventaja de motor.

Se recomputaron independientemente la fórmula de costes y el nivel objetivo desde los eventos y el resultado guardado: coincidencia con todas las sensibilidades. La trayectoria OHLC completa no se ha reproducido independientemente en esta revisión porque el artefacto actual no conserva sus CSV5m. Se distingue la auditoría aritmética de la auditoría de precios.

RUNE-15m-1792169099999 figura NOT_EXECUTABLE_AT_REGISTERED_LEVELS según la regla fijada de entrada: no convertir su HIT direccional en una operación ganadora ni imputarle retorno cero. Referencia 0,64 y rango congelado 0,63712–0,64288. El resultado guardado no incluye la apertura concreta que activó la exclusión; queda pendiente verificarla con evidencia OHLC en la próxima recogida. No se inventó ese precio.

## Reserva prospectiva de costes

Se mantiene inicio 16/10 00:00Z, selección, costes y reglas originales. Seleccionadas15: 147 decisiones elegibles y 17 señales del original; 4 admisiones: 1 SIMULATED, 1 no ejecutable, 2 pendientes; otras 13 señales omitidas por ventana activa. Ninguna señal alternativa dentro de la reserva. XRP: 14 decisiones elegibles, 1 señal original admitida pendiente, sin señales alternativas. Cero pares estrictos de costes resueltos.

Ventanas pendientes de ejecución:
- XRP30m: hasta hoy 18/10 17:24:59.999Z (19:24:59.999 Madrid).
- RUNE1h: hasta hoy 17:39:59.999Z (19:39:59.999 Madrid).
- ADA4h: hasta 20/10 17:39:59.999Z (19:39:59.999 Madrid).

Esperar datos oficiales completos y nuevas recogidas; los horarios configurados no garantizan una ejecución por hora. No hay órdenes reales ni rentabilidad demostrada.

## Mejora de trazabilidad implementada y comprobada

Se leyó de nuevo íntegramente el documento original de 59 puntos antes de modificar código. Siguiendo la exigencia de reproducibilidad, conservación y exportación (7/44/53), el cambio es sólo evidencia aditiva en evaluation/execution_costs.py, no una alteración del protocolo económico.

Commit [c1dcd11](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/commit/c1dcd11e1d9ff8d5e4c2567ee16a6ad3c8b74672):
- Para SIMULATED, SIMULATED_INTERVAL y NOT_EXECUTABLE se adjunta la ventana completa de velas OHLC normalizadas, incluso después del primer toque.
- Se guardan nombre/hash del CSV nativo validado, límites de ventana y SHA256 de la representación JSON normalizada. No es el hash de los bytes CSV originales.
- No se añaden precios a filas pendientes; una ventana resuelta sin todas las velas produce error, nunca relleno.
- Las pruebas comprueban reproducción del resultado desde las velas, conservación de la apertura del caso no ejecutable y rechazo de huecos. Se comprueba que añadir evidencia no cambia el resultado existente.
- [39104080412](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39104080412) success: 90 pruebas Python, incluidas 20 de costes.
- La conservación nueva está publicada; las velas de los casos actuales aún no están en su snapshot antiguo. Verificar la primera recogida que la ejecute antes de afirmar que esos precios ya se han archivado.

No se cambiaron modelos, indicadores, umbrales, reserva temporal, costes, política de admisión ni etiquetas. Android/producción permanecen iguales.

## Evidencias y límites de auditoría

Se recuperaron primero GitHub, PR9, ejecuciones, diarios y artefactos. Auditoría de 423 decisiones seleccionadas y 44 XRP: IDs únicos, SHA256 de evento coincidente con recibo, todos los recibos anteriores a first_open y hashes de modelos/protocolo/motor coincidentes con locks.

Se descargó realmente el ZIP 12125902870: SHA256 calculado 51affad2e0c7700f3327a3a7aaf6a0dfa5c0d3e8d62e7266402fe438b3434398, idéntico al digest de GitHub. El manifiesto dentro del ZIP coincide con official_manifest_sha256 del resultado de costes. El ZIP contiene manifiesto, eventos/modelos/snapshots y pruebas, pero cero CSV5m. No se ejecutaron los pickle de modelos.

Los hashes de diario, recibos y locks de cada snapshot también coinciden con sus bytes recuperados. Se verificó la aritmética de TRX, pero no se afirma una reconstrucción independiente de toda la trayectoria de precios.

- [Seleccionadas15 39099129438](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39099129438): success; recuperó 414 y añadió 9, total 423; 87 pruebas Python anteriores al cambio y compilación/testDebugUnitTest Kotlin correctos.
- [XRP 39102544327](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39102544327): success; recuperó 43 y añadió 1, total 44; 87 pruebas y compilación/pruebas Kotlin correctos.
- Artefacto XRP 12127456454, sha256:0502df5e19d03429af93bdc099ad49d468b8917248d717a39d05fbf741abf82e, vigente.
- Diarios fuente: seleccionado blob 3b567443d0fd3e8312b0f62a4c00db7bd997dc02; XRP b2d977d3640606fc725924d889ebd7b1afd6c6ed.
- Antes de publicar se comprobó ausencia de escritores en curso/en cola. Actualización fast-forward con expected_sha, force=false; sin sobrescribir trabajo ajeno.
- No se repitió matriz150, duplicó recogida, reentrenó ni rellenó mercado. PR9 continúa abierta/no fusionable, sin fusión ni cambio de producción.

Diagnóstico de probabilidad de todas las puntuaciones: XRP Brier 0,246514 (37); TRX15m 0,249670, 30m 0,249696, 1h 0,251064 (28 cada uno), 4h 0,256841 (22). Son puntuaciones dependientes, muchas bajo umbral, no operaciones independientes ni evidencia de calibración estable.

## Próximo paso

Verificar el primer snapshot con evidencia OHLC y reproducir ambos casos iniciales a partir de ella, especialmente la apertura RUNE no ejecutable. Comparar esa nueva fuente con manifiestos y resultados anteriores, sin sobrescribir discrepancias desconocidas. Resolver XRP/RUNE sólo al madurar sus ventanas y continuar la reserva sin ajustar a estos resultados vistos. No hay bloqueo total, ventaja predictiva independiente ni rentabilidad demostrada.
