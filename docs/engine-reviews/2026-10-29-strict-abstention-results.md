# Filtro estricto desde el29/10/2023: resultado negativo

## Qué se comprobó

Primer experimento de abstención sobre señales originales XRP30m, no todos los activos. Fecha simulada29/10/2023 00UTC; datos oficiales archivados desde02/10/2020, decisiones evaluables desde14/06/2021 por preparación/contexto, final02/10/2026 15UTC. Se reutilizan las decisiones as-of y las etiquetas Kotlin del replay40580602201; no se repite matriz150 ni se presenta como nueva reserva independiente o nueva descarga completa de precios.

Protocolo fijado ANTES de resultados en66f3e358a4c7e7480723b451bdd31e26a1c80a95. Código7e2814f20575072dbaa12b7eadbe5b4922916c5e. Ejecución40941462683. No ajustes post-examen. Predictor y criterio mantienen sus hashes. No cambia producción ni reentrena los modelos prospectivos.

## Resultado central

39grupos de tres familias fijas: dirección; dirección+régimen; dirección+régimen+objetivo. Ningún grupo cumple la preselección completa en desarrollo Y validación anterior al examen. Ninguno supera la admisión estricta. Por tanto, las tres familias aceptan0de5619señales del original en el examen: cobertura0%, precisión INDEFINIDA, nunca100%.

No se ha encontrado una mejora utilizable. No se activa el filtro: silenciar todas las alertas no satisface el objetivo del usuario.

| En el mismo examen | Original sin filtro | Dirección | Dirección+régimen | Dirección+régimen+objetivo |
|---|---:|---:|---:|---:|
| Señales |5619|0|0|0|
| Resueltas |5618|0|0|0|
| HIT exacto |4325|0|0|0|
| Aproximadas |524|0|0|0|
| Tardías (no acierto) |245|0|0|0|
| Fallos |524|0|0|0|
| Pendientes |1|0|0|0|
| Exactos sobre resueltas |76,98%|Indefinido|Indefinido|Indefinido|
| Exactos+aproximados |86,31%|Indefinido|Indefinido|Indefinido|

Mantener las categorías separadas: HIT objetivo completo24h; APPROXIMATE al menos75%48h; LATE mínimo entre48–120h; FAIL no mínimo120h. Ningún objetivo ni plazo se relaja para mejorar porcentajes. Esto no evalúa beneficio de operaciones.

## Por qué se abstuvo

En39grupos por2fases=78evaluaciones,74quedan por debajo del95%exacto exigido en sus señales maduras;70no alcanzan90%en primeras señales de episodio;48tienen menos100señales maduras;47menos10primeros episodios maduros;22menos6bloques14d. Los motivos se solapan, no se suman como casos independientes.

Las78evaluaciones tampoco alcanzan el límite Wilson ajustado mínimo90%. Sin embargo, **incluso antes de ese control adicional ya no hay ningún grupo que pase toda la preselección en ambas fases**. No atribuir el resultado exclusivamente a Bonferroni. No rebajar límites ni escoger otra celda después de ver el examen.

Estos recuentos describen la evidencia anterior al29/10/2023; no se usaron resultados futuros para admitir grupos. El número de grupos y ajuste múltiple se congelan antes del examen. Las predicciones se escriben antes de cargar las etiquetas del examen.

## Original como referencia, no filtro aprobado

BUY:2348señales,2347maduras,1753exactas=74,69%;230aproximadas,112tardías,252fallos,1pendiente. SELL:3271maduras,2572exactas=78,63%;294aproximadas,133tardías,272fallos.

Primera señal de cada episodio transitivo120h por dirección en el original:76episodios,64HIT,3aproximados,1tardío,8fallos=84,21%exactos. Es otro denominador, no comparar directamente con76,98%como si fueran dos motores. Agrupar ventanas no demuestra independencia estadística.

## Interpretación y límites

Verificación final: run40941462683 SUCCESS;152pruebasPython y Android testDebugUnitTest assembleDebug correctos. El resultado remoto reproduce el local. Se archivan aquí report.json y frozen-filter.json para conservar conclusiones y reglas fuera del artefacto temporal.

La hipótesis de filtrar por estos grupos con estas exigencias no produce alertas utilizables. No demuestra que ningún filtro futuro pueda funcionar; sí impide presentar este como mejora o vender90/100%de fiabilidad. Rechazar señales con evidencia insuficiente es correcto, pero cero cobertura no es éxito predictivo.

Wilson con corrección múltiple es sensibilidad binomial bajo una hipótesis de independencia no demostrada. Ni superar el límite habría certificado una probabilidad real90%. Histórico ya examinado, dependencia temporal, selección de método retrospectiva y noticias ausentes neutrales permanecen como límites. No aplicar modelos aprendidos en2023a2020.

Conclusión práctica: mantener el original y las recogidas prospectivas sin cambios; conservar este resultado negativo. Otro experimento deberá ser una hipótesis distinta, preregistrada y validada en señales futuras o reserva realmente no usada, no otra relajación sobre este examen.
