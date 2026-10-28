# Capital100 — primera simulación, 28/10/2026

## Resultado y alcance incompleto

Simulación nueva de cartera spot sobre precios reales XRPUSDT y alertas originales cronológicas, desde01/01/2023 hasta02/10/2026 15UTC. Cuenta100 unidades USDT normalizadas, NO100€ reales. Falta cambio EUR/USD, desviación USDT/USD y completar02–28octubre para responder exactamente a euros hasta hoy. No presentar esta fase como petición íntegramente completada.

Reglas fijadas antes de resultados en protocolo938860d8d81fc5e4e98e022ec9cbf7cd396be24b. Una sola posición, compras25% del efectivo con BUY aceptada, cierre total con SELL aceptada, sin cortos, préstamos o apalancamiento. Margen de una vela5m para ejecución, comisiones0,1% y slippage0,05% por lado supuestos. Confianza81 del motor no significa81% de probabilidad certificada. No etiquetas de resultado para elegir operaciones, ni reentrenamiento de modelos2023 aplicado retrospectivamente.

| Escenario fijado previamente | Patrimonio final marcado | Rentabilidad | Caída máxima observada |
|---|---:|---:|---:|
| Alertas,25% del efectivo |134,91|+34,91%|24,40%|
| No vender con pérdidas,25% |177,68|+77,68%|20,58%|
| Mantener efectivo |100,00|0%|0%|
| Comprar y mantener25% inicial |186,46|+86,46%|57,03%|
| Comprar y mantener100% |445,83|+345,83%|72,92%|
| Alertas25%, costes adversos0,2% comisión y0,2%slippage/lado |114,16|+14,16%|29,77%|

Los comparadores no igualan exposición o riesgo: comprar100% no es un rival con el mismo capital en riesgo. El25% inicial de buy&hold también deriva su peso con el mercado. Comparación descriptiva, no prueba estadística de superioridad ajustada por riesgo.

## Principal: alertas25%

134compras y133ventas.20operaciones cerradas ganadoras y113perdedoras (15,04% ganadoras), pero las ganancias mayores compensan las pérdidas: esto no es la tasaHIT de objetivos y muestra por qué precisión del pronóstico y rentabilidad son cosas distintas.

Efectivo final97,73786; posición24,56428XRP valorada37,17067. Ganancia realizada30,31715 y latente4,59138; comisión pagada7,83484 ya descontada. Patrimonio134,90853; liquidación final hipotética tras costes134,85279, no venta realmente ejecutada. No sumar de nuevo comisiones ni ganancias al patrimonio.

## Variante no vender en pérdida

8compras y7ventas, las7ganadoras por construcción.3895alertas de venta se ignoran al no cubrir coste de adquisición neto. No es100% de fiabilidad: queda posición abierta16,30626XRP, coste51,00087 y valor24,67464, pérdida latente26,32624. Efectivo153,00262. Patrimonio177,67726; liquidación hipotética177,64026.

## Límites, verificaciones y evidencia

- Sólo XRP30m con patrones históricos originales as-of; no todos los activos ni réplica de un móvil con noticias.
-143pruebasPython y Android testDebugUnitTest assembleDebug correctos.
-Run40761468499 SUCCESS, commit590ec456911e32e36face0c132ec1023507e2d81.
-Artefacto12848795518, SHA25661cc2197ad8ab78d21087b0d2ca4661a72fde4d9240f738b8c19082197ec01be: libro de operaciones, descartes, patrimonio diario y manifiestos.
-394723velas5m observadas y17faltantes durante el intervalo. Nunca se rellenan; ausencia de vela exacta cancela una orden. Las caídas se miden sólo a cierres5m observados, no todos los mínimos intravela ni durante huecos.
-Cantidades fraccionarias ideales y mínimo supuesto5unidades; filtros históricos de lotes, profundidad y tarifas reales no reconstruidos. Sin impuestos ni costes de conversión.
-El histórico ya inspeccionado no es una validación independiente nueva. No optimizar25% ni reglas mirando esta tabla.
-No cambio de producción ni operaciones reales. Ganancia pasada hipotética no garantiza ganancia futura.

## Conclusión y trabajo pendiente

Principal gana34,91% nominal en esta muestra, pero menos que comprar y mantener25%, a cambio de menor caída observada. Costes mayores reducen ganancia a14,16%. La variante sin pérdidas realizadas conserva una pérdida abierta importante. No demuestra90% de precisión ni una ventaja independiente del motor.

Para completar la petición: incorporar referencias FX verificadas y distinguir USDT deUSD, y reconstruir las velas y alertas02–28octubre con los mismos candados, sin introducir señales retrospectivas en los diarios prospectivos. Esta fase no inventa esos datos ni los declara hechos.
