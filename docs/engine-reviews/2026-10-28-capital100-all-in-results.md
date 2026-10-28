# Capital100 — reinversión del100% por alertas

## Petición y método

José Domingo solicita reinvertir todo el saldo tras cada venta (100→110→siguiente compra110). No se interpreta su ejemplo10% como take-profit fijo. Protocolo y código publicados antes del resultado en cc4897aed1d608402cc81c58b418baae595ccadb. No se modifica predictor ni producción.

Mismo archivo oficial XRPUSDT,01/01/2023–02/10/2026 15UTC, mismas alertas originales as-of y margen5m de ejecución que el experimento25%. Cuenta100 unidades USDT normalizadas, NO100€ reales ni resultado hasta28/10. Continúan pendientes FX y últimos26días. Sin apalancamiento ni aportaciones. Compra100% del saldo incluyendo comisión; venta100% y siguiente compra reinvierte neto.

## Resultados, patrimonio marcado

| Variante | Final desde100 | Retorno | Caída máxima a cierres5m observados |
|---|---:|---:|---:|
| Alertas25% (control anterior reproducido) |134,90853|+34,91%|24,40%|
| Alertas100% reinvertido |100,68784|+0,69%|66,57%|
| Alertas100%, costes adversos |51,65191|-48,35%|75,33%|
| No vender en pérdidas100% |394,64919|+294,65%|73,09%|
| Comprar y mantener100% |445,83442|+345,83%|72,92%|

Comisión principal0,1% y slippage0,05% por lado; adversos0,2%/0,2%. Escenarios supuestos, no tarifas históricas verificadas. No se puede multiplicar por cuatro el beneficio25%: el100% compone también pérdidas y costes sobre una base variable.

## Principal100%

134compras,133ventas,20ganadoras y113perdedoras. Misma selección de alertas que el25%, importes distintos. Comisiones27,99485 unidades, ya descontadas. Ganancia realizada -11,74928; ganancia latente12,43712. Efectivo0, posición abierta66,53968XRP, coste88,25072 y valor100,68784. La liquidación hipotética neta final sería100,53686, no una venta ejecutada. No confundir valor de posición con saldo bancario realizado.

La venta se realiza con SELL aunque haya pérdidas. El resultado20/133 no equivale al acierto exacto del pronóstico; son operaciones de cartera completas entre alertas.

## Variante100% sin vender pérdidas

8compras y7ventas, las7ganadoras por construcción.3895alertasSELL ignoradas por no recuperar neto el coste de adquisición. Efectivo0; posición abierta260,80438XRP con coste815,71423, valor394,64919 y **pérdida latente421,06504**. Liquidez hipotética final394,05742 después de costes de salida. Ganancia realizada acumulada715,71423 fue reinvertida; NO sumar esa cifra al patrimonio final.

Por tanto, no decir que hay815,71 disponibles ni100%de fiabilidad. La prohibición de cerrar pérdidas puede ocultar pérdidas abiertas muy grandes; aquí se incluyen expresamente. Buy&hold y esta variante no igualan exposición ni riesgo del25%.

## Verificación y límites

Run40781626292 SUCCESS, commitcc4897aed1d608402cc81c58b418baae595ccadb:145pruebasPython y Android testDebugUnitTest assembleDebug correctos. Prueba de reinversión100→110→121 sin costes y prueba de presupuesto con comisión incluida.

Artefacto12857457626, SHA25618ee52e00820f5aba792ffb3e5abcc19840b383e8ca1430275e153f18ea53810: operaciones, descartes, patrimonio diario, manifiestos y tests. Control25% reproduce exactamente134,9085263105292. Los resultados JSON se conservan también en esta rama.

394723velas5m observadas y17ausentes; no rellenadas. Órdenes sin vela exacta se cancelarían. Caída máxima no incluye mínimos intravela ni observación durante huecos. Cantidades fraccionarias ideales; mínimo supuesto5unidades, sin reconstrucción de filtros históricos de lotes/profundidad, impuestos, FX ni desviaciónUSDT/USD.

Resultado exploratorio en histórico inspeccionado: no garantiza rentabilidad futura, no prueba ventaja independiente y no es recomendación para invertir100%. Esta simulación no ejecuta órdenes reales. El10% del ejemplo no se introdujo como salida nueva.
