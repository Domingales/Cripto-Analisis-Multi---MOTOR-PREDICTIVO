# Confluencias anteriores al movimiento — resultado negativo29/10/2026

## Método y alcance

José Domingo pide encontrar condiciones que aparecen juntas antes del movimiento, agruparlas y volver a experimentar. Se estudian TODAS las ventanas evaluables, no sólo alertas originales, con BUY/SELL separados. Primera fase XRP30m; archivo oficial desde02/10/2020, primeras ventanas14/06/2021, corte02/10/2026 15UTC. Fecha simulada de examen29/10/2023. Histórico previamente inspeccionado: exploratorio, no reserva independiente nueva.

Protocolo b052165713287f444d51956d00c907e0682209a3 publicado antes de resultados; código5eaf43bc8c012149bd7dc989df0c3e9a79b88f33. No reentrenar modelo aprendido en2023 sobre2020; reutilizar condiciones as-of y etiquetas Kotlin del run40580602201, ZIP verificado4738051e6fd0538d1c45b3c0a409c55d03f9c62ff3ffd355a7540bfc303e0184. Predictor y ForecastCriterion verificados por sus candados. No matriz150 repetida.

Ocho condiciones fijas:volumen>=1,5media,ADX>=25,MTF>=8alineado,precio frente media50alineado,retorno4velas30malineado,RSIdireccional,régimen de tendencia alineado,ATR%moderado0,5–2. Conjunciones de2/3:84por dirección,168en total. ATRmoderado NO expansiónATR,ADXalto NO creciente,media50es media aritmética del archivo NO EMA50. No se han medido ruptura exactaSR/pendientes que faltan en esa extracción.

Desarrollo: horizonte120h maduro antes01/04/2023. Validación reservada anterior al29/10/2023 con purga120h; examen posterior separado. Selección prevista top3por dirección exclusivamente en desarrollo, exigiendo soporte y al menos3puntos de mejora frente todas las ventanas de su misma dirección. Después validación>=90%exacto y>=3puntos sobre su referencia; no sustitución postvalidación. Esos90%no son probabilidad certificada.

## Resultado central

**Ninguna de las168combinaciones alcanza la mejora mínima de3puntos en desarrollo.** Por ello no se seleccionan reglas ni se promocionan a validación, y el candidato congelado permanece vacío. No se ha encontrado una mejora utilizable ni una fiabilidad90/100%. No se rebajan límites ni se escoge el mejor porcentaje del examen.

| Descripción EN DESARROLLO, no resultado de examen | Maduras | HIT | Exactos | Referencia de igual dirección | Diferencia |
|---|---:|---:|---:|---:|---:|
| Mejor precisiónBUY:volumen>=1,5+ADX>=25+ATRmoderado |2320|1780|76,72%|75,84%|+0,88puntos|
| Mejor precisiónSELL:ADX>=25+ATRmoderado |15949|12330|77,31%|78,44%|-1,13puntos|

Son los máximos aparentes entre84pruebas por dirección; no mejoras independientes demostradas. Otros grupos tienen precisiones inferiores. No afirmar que combinar indicadores en general sea inútil: sólo este conjunto finito y estos umbrales no funcionaron según el protocolo.

## Falsas alarmas y movimientos no detectados

Para la combinaciónBUY anterior:2323apariciones,2320maduras;1780exactas,234aproximadas,99tardías,207fallos y3pendientes. Si el objetivo es HITexacto, hay540noHIT entre las maduras; no llamarlos todos FAIL porque incluyen aproximados y tardíos.

En desarrollo todas las ventanasBUY suman26345maduras y19981HIT. Esa combinación cubre1780de esos19981HIT y deja18201fuera. Tabla de contingencia por ventana:1780verdaderos positivos,540falsos positivos para HITexacto,18201falsos negativos,5824verdaderos negativos. No son movimientos independientes: son ventanas con objetivos adaptativos originales.

Para la combinaciónSELL:16029apariciones,15949maduras;12330exactas,1830aproximadas,715tardías,1074fallos y80pendientes. Su tasa exacta es menor que la referenciaSELL aun con muchos datos.

## Solapamiento e incertidumbre

PrimeraBUYespaciada estrictamente más120h de la anterior:102casos,82exactos=80,39%,10aproximados,1tardío,9fallos. Wilson95%descriptivo inferior71,65%; ajustado168pruebas63,11%. No son límites certificados del mercado; hay dependencia y deriva.

Agrupar transitivamente esas apariciones produce sólo2episodios con primera señalHIT en ambos. **Ese2/2no es un100%fiable**. Las ventanas densas encadenan episodios largos; tampoco el espaciado demuestra independencia. Se conservan ambos desgloses, sin elegir el más atractivo.

## Periodo posterior y referencia original

Examen29/10/2023–02/10/2026:51342ventanas evaluables. Como no hay reglas admitidas,0alertas candidatas,0%de cobertura,precisión indefinida. No se ha probado una regla válida en ese examen porque ninguna pasó desarrollo; no confundir ese hecho con falta de datos del examen.

Original sin nuevo filtro:5619alertas,5618maduras,4325exactas=76,98%,524aproximadas,245tardías,524fallos,1pendiente. Son otra población de selección; no comparar directamente con los porcentajes de DESARROLLO de la tabla para declarar un ganador.

El criterio mantiene objetivo medio del rango original, HIT24h,aproximado75%48h,tardío48–120h,fallo120h y pendientes por huecos. BUY/SELLpueden acertar ambos en una ventana; no son complementarios. No resultados de inversión, ejecución o costes.

## Verificación y archivo

Run40957947597 SUCCESS:158pruebasPython y Android testDebugUnitTest assembleDebug correctos. Artefacto12933863974 SHA256d2ba2dd8ddb2e4b58c5bb183f1b4fdc884e664958a840f1a03683093748d3d5b contiene búsqueda,reglas,predicciones previas a etiquetas y resultados. development-search.json con todas168reglas, frozen-rules.json y report.json se conservan además en esta rama.

No se introduce ninguna regla en producción ni se cambian modelos,umbrales,alarmas,persistencia o recogidas prospectivas. El resultado negativo se conserva para evitar repetirlo como novedad. Otra hipótesis deberá fijarse por adelantado y necesitará señales futuras/reserva realmente no usada, no escoger otra conjunción después del examen.
