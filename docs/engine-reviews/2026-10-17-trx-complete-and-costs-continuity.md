# Seguimiento prospectivo — 17 de octubre de 2026, 08:19 Madrid

Las ocho señales alternativas de TRX están resueltas: 4 HIT y 4 FAIL; las siete de 4h dan 3 HIT y 4 FAIL. No hay mejora predictiva independiente demostrada. El avance técnico comprobado es la continuidad del postprocesado de ejecución/costes dentro de las recogidas completas.

## Estado actual

| Cohorte | Decisiones | Señales | HIT | FAIL | NEUTRAL | Pendientes |
|---|---:|---:|---:|---:|---:|---:|
| XRP30m original | 37 | 4 | 3 | 1 | 0 | 0 |
| XRP30m alternativo | 37 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 357 | 52 | 13 | 21 | 2 | 16 |
| Seleccionadas15 alternativo | 357 | 8 | 4 | 4 | 0 | 0 |
| ADA1h main, cohorte distinta | 62 | 9 | 4 | 4 | 0 | 1 |

Cortes: seleccionadas15 2026-10-17T06:12:27.217Z, XRP 2026-10-17T05:41:45.973Z. ADA recuperada de main después de la ejecución 38998411585, iniciada 03:20:38Z y terminada success. Las cohortes no tienen los mismos activos/entradas/horizontes; sus porcentajes no son una comparación emparejada.

Respecto al cierre del 16: seleccionadas15 +66 decisiones y +7 señales originales; original +3 HIT y +11 FAIL; alternativo +3 FAIL sin señales nuevas. XRP +6 decisiones y +1 HIT, sin señales nuevas. ADA +4 decisiones, +1 señal y +2 HIT, sin nuevos FAIL.

## TRX: resultado completo y comparación justa

| Decisión aceptada por ambos | Original | Alternativo |
|---|---|---|
| TRX-4h-1791734399999 | FAIL | HIT |
| TRX-4h-1791835199999 | HIT | FAIL |
| TRX-4h-1791907199999 | HIT | FAIL |

Los tres casos pareados favorecen dos veces al original y una al alternativo, pero siguen siendo una muestra mínima y dependiente. No demuestra superioridad del original ni del alternativo.

Las ocho compras alternativas siguen formando un único grupo transitivo multitemporal. Su primera señal TRX4h fue HIT; los cuatro HIT compartían una vela de resultado. La agrupación no borra los cuatro FAIL ni convierte este seguimiento en evidencia independiente. No se reescogió una señal tras observar los resultados.

Agrupación adicional por activo/motor: original seis grupos, primeras señales 2 HIT (RUNE), 1 FAIL (ADA), 1 NEUTRAL (TRX) y 2 pendientes (ETH/ATOM); alternativo un grupo con primera señal HIT. XRP mantiene tres episodios originales cuyas primeras señales fueron HIT; sus cuatro señales individuales son 3 HIT/1 FAIL. No solapar no garantiza independencia estadística.

## Calibración y diagnóstico por régimen

XRP alternativo: cero señales aceptadas de un modelo evaluable; 31 puntuaciones resueltas, Brier 0,245849.
TRX alternativo: Brier 15m 0,261098; 30m 0,260930; 1h 0,262097 (23 puntuaciones por plazo); 4h 0,245721 (17 puntuaciones). No extrapolar a muestras independientes ni confundir puntuaciones bajo umbral con operaciones. El Brier 4h pasó de 0,126334 en 9 puntuaciones el día 15 a 0,245721 en 17: la aparente ventaja inicial no se ha mantenido en ese diagnóstico descriptivo.

Se extrajo el régimen almacenado en el snapshot de cada decisión, sin reconstruirlo con datos futuros. Ejemplos descriptivos de seleccionadas15:

| Motor, régimen y dirección | Señales | HIT | FAIL | NEUTRAL | Pendientes |
|---|---:|---:|---:|---:|---:|
| Original TREND_DOWN SELL | 26 | 7 | 13 | 2 | 4 |
| Original MIXED SELL | 11 | 1 | 5 | 0 | 5 |
| Original BREAKOUT SELL | 7 | 2 | 3 | 0 | 2 |
| Alternativo TREND_DOWN BUY | 6 | 3 | 3 | 0 | 0 |
| Alternativo RANGE BUY | 1 | 1 | 0 | 0 | 0 |
| Alternativo BREAKOUT BUY | 1 | 0 | 1 | 0 | 0 |

Estas filas son diagnósticos exploratorios, no una selección exhaustiva de ganadores ni una propuesta de filtro. Mezclan activos/plazos y ventanas dependientes. Los casos únicos RANGE/otros regímenes no justifican especialización, optimización de umbrales o cambio en producción. Mantener reserva futura y corrección de selección múltiple.

## Ejecución/costes: continuidad ya verificada, sin retornos todavía

Se comprobaron dos ejecuciones completas consecutivas por cohorte con snapshots guardados en GitHub:
- Seleccionadas15: 38990980529 y 39008490135; commits de costes a6c3f8a9ce260a9b299560d168d00ba97db55a64 y bbc0b116a1195b26ca2b10513b2e9d9707b75ac5.
- XRP: 38986945034 y 39007071650; commits 08b9ef405ab6c59b873c8cea15d180fff2ce46d1 y 59fb05b4937edb60797c29aaec94df3e57326379.

La reserva desde 16/10 00:00Z permanece fija. Seleccionadas15 tiene 81 decisiones elegibles (antes 72), 7 señales aceptadas del original: dos casos admitidos para simulación y cinco omitidos por ventana activa; el alternativo no tiene señales en la reserva nueva. XRP tiene 7 decisiones elegibles (antes 6), sin señales de ambos motores.

Los dos casos admitidos siguen PENDING_DATA/WINDOW_NOT_CLOSED, no son operaciones reales ni retornos calculados:
- RUNE15m: primera vela 16/10 17:05Z; horizonte hasta hoy 17/10 17:04:59.999Z, es decir, 19:04:59.999 Madrid.
- TRX30m: primera vela 17/10 01:40Z; horizonte hasta 18/10 01:39:59.999Z, es decir, mañana 03:39:59.999 Madrid.

Ningún resultado de ejecución está resuelto y no hay comparación pareada de costes disponible. Esperar la ventana completa aunque haya un toque temprano. No contar como FAIL un hueco de datos ni reutilizar los ocho resultados TRX anteriores a la reserva.

Se recomputaron localmente filas, elegibilidad, omisiones y pares desde los diarios/recibos al instante del snapshot: coincidencia exacta. No se necesitaron precios para reproducir estas filas aún inmaduras; no se adjudican precios ni retornos. Se verificaron también los hashes de diario, recibos y freeze-lock frente a los publicados. Los snapshots periódicos incorporan además hash del manifiesto oficial y corte de mercado.

## Auditoría, ejecuciones y artefactos

Fuentes recuperadas primero de GitHub, no de scratch. Auditoría de 357 decisiones seleccionadas y 37 XRP: IDs únicos; SHA256 de cada evento coincide con su recibo; recorded_at <= confirmed_at < first_open en todos; modelo, protocolo y motor coinciden con los locks congelados. No se modificaron código, modelos, protocolos ni umbrales en esta revisión.

- [Seleccionadas15 39008490135](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39008490135): success; recuperó 348 y añadió 9, total 357. 87 tests Python y compilación/testDebugUnitTest Kotlin correctos.
- [XRP 39007071650](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/39007071650): success; recuperó 36 y añadió 1, total 37. 87 tests Python y compilación/pruebas Kotlin correctos.
- Las dos recogidas precedentes 38990980529 y 38986945034 también success, con 87 tests y compilación/pruebas Kotlin correctos.
- Artefacto seleccionado 12092137708, sha256:2ef7ffed21b763be01a188b1fae64cb416bfe57f13a92560cee61f53e1b2db63; XRP 12090569151, sha256:f1fbf67f97c1f053969d1186814ed8a6dba7d9defbd99b7d7d3a5d64ca8bb98c. Vigentes. Diarios/snapshots quedan también en la rama, no sólo en artefactos.
- Blobs de diarios auditados: seleccionado 8b06749e7d8e25c343b57d5d31b2ca9917beb392; XRP cdf9b28a2b32f9d94d54a9764a5e03640a91512e.
- Antes de publicar se verificó que no había ejecuciones en curso ni en cola. No se duplicaron recogidas, forzaron refs, repitió matriz150 ni rellenaron decisiones retrospectivas.

PR9 sigue abierta y no fusionable. No se fusionó ni hace falta hacerlo para la investigación. Producción Android, identidad visual, funciones, alarmas y vigilancia híbrida permanecen intactas. La configuración horaria sigue presentando intervalos efectivos de varias horas; no afirmar vigilancia horaria garantizada.

## Siguiente paso

Resolver primero la ventana RUNE de costes después de las 19:05 Madrid, sólo cuando la recogida disponga de datos oficiales completos, y después TRX. Auditar entrada/salida/costes e incertidumbre intravela antes de interpretar retornos. Continuar informes por régimen/bloques con resultados futuros, sin convertir este diagnóstico exploratorio en un filtro entrenado sobre datos ya vistos. No hay bloqueo técnico total, mejora predictiva independiente ni rentabilidad demostrada.
