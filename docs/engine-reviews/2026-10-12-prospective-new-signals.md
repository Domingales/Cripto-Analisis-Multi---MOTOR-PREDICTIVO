# Seguimiento prospectivo — 12 de octubre de 2026

Revisión a las 08:21 Europe/Madrid de registros durables y ejecuciones actuales. No se repite la matriz150, no hay reentrenamiento ni cambios de umbrales o motor de producción.

## Resultados comprobados

| Cohorte / motor | Decisiones únicas | Señales | HIT | FAIL | NEUTRAL | Pendientes |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| XRP30m original | 14 | 3 | 2 | 1 | 0 | 0 |
| XRP30m alternativo | 14 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 93 | 7 | 0 | 0 | 0 | 7 |
| Seleccionadas15 alternativo | 93 | 4 | 0 | 0 | 0 | 4 |
| ADA1h original (main) | 44 | 4 | 2 | 0 | 0 | 2 |

XRP añade cinco decisiones desde la revisión anterior. La tercera venta es HIT. Precisión descriptiva por señal: 2/3 (66,67%); por primera señal de episodio: 2 HIT en dos episodios, cero FAIL. Dos episodios no demuestran independencia estadística ni mejora. El alternativo XRP sigue sin señales aceptadas: nueve puntuaciones resueltas, Brier 0,248106; probabilidad media 0,517339 y frecuencia observada 0,555556. Muestra mínima, sin ventaja demostrada.

La cohorte15 añade 36 decisiones y tres señales originales. El alternativo emite sus primeras cuatro señales: BUY TRX1h (una) y BUY TRX4h (tres), probabilidades congeladas 0,669417 y 0,657789/0,722384/0,723082. Son puntuaciones del modelo, no porcentajes de éxito demostrados. En TRX4h existen direcciones enfrentadas con el original; conservar ambas, sin seleccionar retrospectivamente la acertada. Ventanas de TRX4h solapadas no representan tres pruebas independientes.

ADA añade cuatro decisiones y una señal BUY desde el informe anterior.

## Ventanas abiertas frente a resultados esperando recogida

La última recogida15 terminó a las 02:00:56 Madrid; su resumen se calculó antes del vencimiento a las 03:24:59 Madrid de TRX30m SELL, TRX1h SELL y RUNE1h SELL. Esas tres señales siguen pendientes en el registro: todavía no hay una recogida posterior comprobada para resolverlas. No se atribuye el retraso a huecos de datos sin evidencia.

ADA se calculó a las 06:46 Madrid con datos cerrados hasta las 06:00 Madrid. Su SELL venció a las 06:34:59, por lo que no disponía de toda la ventana oficial en esa ejecución. El BUY vence el 12 de octubre a las 21:59:59 Madrid.

Otras ventanas originales15: TRX1h vence hoy a las 09:54:59 y 17:14:59; TRX4h el 14 de octubre a las 21:49:59; ATOM1d el 24 a las 08:34:59. Alternativo TRX1h vence el 13 a las 02:04:59; TRX4h el 14 a las 17:14:59 y 21:49:59, y el 15 a las 02:04:59.

## Continuidad, pruebas y evidencias

- XRP: run38211060032 success, recupera 13 decisiones y añade una sin pérdida; modelo/protocolo iguales a los congelados. 70 pruebas Python y testDebugUnitTest pasan en esta ejecución.
- Seleccionadas15: run38185953833 success, recupera84 y añade9 hasta93. Protocolo intacto.
- ADA: run38205042320 success.
- Artefactos actuales: XRP11728919802 (SHA256 85e88d94a36d51213189a77096f8e8a9f4c39ddce17221b393b349053fdc5b03), seleccionadas11719986074 (9824ee8b72e8fd7bd82d25a2dbda559658b12398ddd3bd37e624d5145bc1fdb1), ADA11726618637 (7ad3bb0088164998e769a28d76ad220f8d4fd6afec0ee038190ead3c4abe8e32).
- Los lanzadores main declaran cron horario (:17 XRP, :37 seleccionadas), pero las ejecuciones observadas no garantizan cadencia horaria efectiva: hay intervalos de varias horas. No rellenar velas omitidas ni llamar a esto ejecución continua.
- PR9 sigue abierta/no fusionable. Ninguna fusión realizada; este estado no impide la recogida separada.
- No se ha cambiado código; no se presenta una nueva compilación APK como realizada. La compilación Android citada anteriormente fue run38030613499; hoy se verificó el test de dominio Android.

## Próximo trabajo y límites

Resolver las tres ventanas15 vencidas y ADA SELL en la siguiente recogida con datos oficiales completos, preservando pendientes ante huecos. Mantener congelados modelos y reglas pese a las primeras señales TRX. Para la posterior simulación de costes, fijar antes de usar más resultados: entrada ejecutable posterior a escritura durable, spread/deslizamiento/comisiones por separado, una posición por episodio y tratamiento conservador de ambigüedad intravela; no convertir MFE/MAE en rentabilidad.

No hay mejora predictiva independiente demostrada. Estos resultados no justifican introducir el alternativo en producción.

Fuentes: evaluation/prospective-xrp/{summary.json,episodes.json,collection-runs.json}; evaluation/selected-prospective/{summary.json,events-2026-10.jsonl,collection-runs.json}; evaluation/prospective/events.jsonl en main; workflows y logs de las ejecuciones citadas.
