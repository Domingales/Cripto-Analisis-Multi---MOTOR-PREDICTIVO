# Auditoría de ejecución — 19 de octubre de 2026, 09:07 Madrid

La evidencia OHLC añadida ayer ya se conserva en las recogidas completas. Se reprodujeron las cuatro ventanas resueltas/no ejecutables desde sus velas guardadas, con implementación de contraste y aritmética decimal. Coinciden exclusión, primer toque, precios de salida y costes. Es un avance de auditoría, no de precisión predictiva.

Hay dos pérdidas simuladas nuevas del original: RUNE1h -0,851752% y XRP30m -0,820569% en el escenario principal. El único caso anterior TRX sigue +0,078902%. No sumar estos retornos como una cartera ni inferir rentabilidad de tres operaciones dependientes.

## Estado direccional

| Cohorte | Decisiones | Señales | HIT | FAIL | NEUTRAL | Pendientes |
|---|---:|---:|---:|---:|---:|---:|
| XRP30m original | 50 | 6 | 3 | 2 | 0 | 1 |
| XRP30m alternativo | 50 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 489 | 68 | 22 | 34 | 2 | 10 |
| Seleccionadas15 alternativo | 489 | 8 | 4 | 4 | 0 | 0 |
| ADA1h main, cohorte distinta | 72 | 14 | 6 | 5 | 0 | 3 |

Cortes: seleccionadas15 2026-10-19T05:19:18.557Z; XRP 2026-10-19T04:55:53.100Z. ADA de main tras 39195548155, iniciada 03:37:13Z y terminada success. No comparar porcentajes de cohortes con entradas/activos/horizontes diferentes como si fueran un experimento pareado.

Desde el cierre del 18: seleccionadas15 +66 decisiones y +6 señales originales; original +2 HIT/+11 FAIL, alternativo sin señales ni resultados nuevos. XRP +6 decisiones, +1 señal y +1 FAIL. ADA +5 decisiones, +3 señales y +2 HIT, sin nuevos FAIL.

El alternativo conserva los ocho resultados TRX (4 HIT/4 FAIL) en un grupo multitemporal solapado cuya primera señal fue HIT. Los tres casos aceptados por ambos motores favorecen dos veces al original y una al alternativo, muestra mínima dependiente. No hay ventaja independiente demostrada ni motivo para cambiar producción.

## Reproducción de precios y costes

Cada una de las cuatro ventanas conserva 288 velas 5m: se verificaron timestamps contiguos, cierre a 299999 ms de la apertura, OHLC positivos/finitos y consistentes, y SHA256 de la representación normalizada. Se reconstruyeron reglas de apertura, salto, doble toque y primer límite; después se calcularon fills y comisiones con Decimal, sin importar la función productora de resultados.

| Caso del original | Estado reproducido | Retorno principal |
|---|---|---:|
| RUNE15m, 16/10 | No ejecutable a niveles registrados | No adjudicar retorno |
| TRX30m, BUY | TARGET | +0,078902% |
| RUNE1h, SELL hipotético | STOP | -0,851752% |
| XRP30m, SELL hipotético | STOP | -0,820569% |

Escenario principal registrado: comisión 0,10% y deslizamiento adverso 0,10%, ambos por lado. Supuestos de simulación, no tarifas verificadas de eToro. Las ventas cortas son hipotéticas, sin financiación/préstamo modelados ni garantía de que el broker permita esa operación.

### Exclusión RUNE15m confirmada
RUNE-15m-1792169099999: referencia 0,64, nivel inferior 0,63712 y superior 0,64288. La primera vela admisible abrió a 0,636, ya por debajo del objetivo SELL. La exclusión es reproducible; su HIT direccional no se transforma en una compra/venta ganadora ni en retorno cero. Ésta era la apertura que faltaba en el snapshot de ayer.

### Sensibilidad de los tres retornos
Fee fijo 0,10% por lado:

| Caso | Slip 0% por lado | Slip 0,10% por lado | Slip 0,20% por lado |
|---|---:|---:|---:|
| TRX30m | +0,279460% | +0,078902% | -0,121256% |
| RUNE1h | -0,650450% | -0,851752% | -1,053458% |
| XRP30m | -0,619329% | -0,820569% | -1,022213% |

TRX entrada 0,3355, salida objetivo 0,3371102. RUNE1h entrada 0,634, salida stop 0,636853. XRP entrada 1,2924, salida stop 1,297814. Se comprobaron nivel, motivo y timestamp de salida, sin escoger retrospectivamente el mejor precio. Los stops de RUNE y XRP ocurrieron el 17/10 a las 18:04:59.999Z y 18:24:59.999Z: no tratarlos automáticamente como pruebas independientes.

Hashes de ventanas normalizadas:
- RUNE15m: 3b6ed37e03efad8d395b8e566582fef3b31d497dc9ebaaaa8057a5546d891136.
- TRX30m: debf46f7ca12b8ed1277535662a6bd4b5f0b95e7fbaf0a125fa27d2a2d5253d4.
- RUNE1h: a1da0cc81d47377dcdf7df6dfb08cf81152aea1d34e610c6a815119fd92fa5bb.
- XRP30m: 4f838a2094c04858ee0e534e8430b22014430d66827b314f97293b0c1f4005cc.

La reproducción contrasta la implementación a partir de la misma evidencia de mercado guardada, no de una segunda fuente independiente. El hash normalizado no es el hash de los bytes CSV originales. Se conserva también la referencia/hash del CSV nativo validado y el manifiesto de la recogida. No se afirma que se hayan recuperado hoy los CSV completos de 120 días ni se inventan precios.

## Reserva de costes y estados pendientes

Inicio 16/10 00:00Z sin cambios. Seleccionadas15: 213 decisiones elegibles y 23 señales originales. Se reconstruyó cronológicamente la política de admisión sin consultar resultados: 17 omitidas por ventana activa y 6 admitidas (1 no ejecutable, 2 simuladas resueltas y 3 pendientes). XRP: 20 decisiones elegibles y 2 señales originales admitidas (1 resuelta, 1 pendiente). Ninguna señal alternativa dentro de esta reserva; cero pares de costes resueltos entre motores.

Pendientes:
- RUNE1h: hasta hoy 19/10 19:44:59.999Z, 21:44:59.999 Madrid.
- XRP30m: hasta hoy 21:39:59.999Z, 23:39:59.999 Madrid.
- ADA4h: hasta 20/10 17:39:59.999Z, 19:39:59.999 Madrid.
- TRX4h: hasta 22/10 00:34:59.999Z, 02:34:59.999 Madrid.

No adelantar ventanas completas aunque haya un toque temprano. Esperar recogida con datos oficiales completos; no convertir huecos en FAIL ni rellenar decisiones omitidas.

## Integridad y ejecuciones

Fuentes recuperadas primero de GitHub. Auditoría de 489 decisiones seleccionadas y 50 XRP: IDs únicos, SHA256 de cada evento coincidente con el recibo, recorded_at <= confirmed_at < first_open en todos, y coincidencia de modelos/protocolos/motor con locks congelados. Hashes de diario, recibos y freeze-lock del snapshot coinciden con sus bytes recuperados.

También se reprodujo el orden de todas las filas de costes: 23 seleccionadas y 2 XRP, incluyendo omisiones por ventana activa. No se cambió admisión después de conocer pérdidas ni exclusiones.

- [Seleccionadas15 39203217066](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39203217066): success; recuperó 480 y añadió 9, total 489. 90 tests Python y compilación/testDebugUnitTest Kotlin correctos.
- [XRP 39201766942](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39201766942): success; recuperó 49 y añadió 1, total 50. 90 tests y compilación/pruebas Kotlin correctos.
- Artefacto seleccionado 12165096846, sha256:04d35f47ebf30568e9c50dbd8d20cbe8bdde2ab3a8c121244c198e8740b2a826; XRP 12164456072, sha256:aff9c06b5136669175cd46e857d0fc7874695be59ad81ec10057615d2ec3576b. Ambos vigentes; diarios y velas normalizadas persisten además en la rama.
- Blobs fuente de diarios: seleccionado 57349c6eb766075bf7276bd49361ec2d5bea3141; XRP d94d4a237e465fd7c9e79dff656a207763f691f1.
- Hoy no se modificó código, ni modelos/protocolos/umbrales/costes. No se repitió matriz150 ni se lanzaron recogidas duplicadas. No se forzaron refs ni se fusionó la PR9.
- PR9 sigue abierta/no fusionable. Producción, interfaz, funciones, alarmas y vigilancia híbrida siguen intactas. La configuración horaria mantiene intervalos efectivos de varias horas; no prometer ejecución horaria garantizada.

Brier de todas las puntuaciones alternativas resueltas: XRP 0,246998 (43); TRX15m 0,258544, 30m 0,258587, 1h 0,258749 (35 por plazo); 4h 0,253879 (27). Muestras dependientes, muchas puntuaciones bajo umbral; sin evidencia robusta de calibración superior.

Agrupación descriptiva: seleccionadas15 original siete grupos (primeras señales 2 HIT, 1 FAIL, 1 NEUTRAL, 3 pendientes), alternativo uno (primera HIT). XRP original cinco episodios (3 HIT, 1 FAIL y 1 pendiente en primeras señales). No solapar no prueba independencia; no seleccionar máximos ni crear filtros con estos resultados ya vistos.

## Próximo paso

Mantener la reserva y resolver las ventanas RUNE/XRP cuando existan datos completos; repetir la reproducción desde evidencia persistida. Acumular operaciones futuras y regímenes distintos antes de estimar incertidumbre por bloques o comparar políticas económicas. Las pérdidas nuevas no se corrigen reentrenando o relajando umbrales sobre esta muestra. No hay bloqueo total, mejora predictiva independiente ni rentabilidad fiable demostrada.
