# Archivo de seis años: motor original XRP30m

## Alcance y procedencia

Se audita el recorrido cronológico ya reconstruido en el run 40580602201, artefacto 12768818184. No se repite la matriz150 ni se presenta la reutilización de resultados como un experimento independiente nuevo. Nueva auditoría reproducible: commit bee26046938e1988a5678f803cbaa5505b352cdf, run 40735689170.

Datos oficiales archivados: 02/10/2020 00:00 UTC–02/10/2026 15:00 UTC. Primer análisis evaluable: 14/06/2021 07:59:59.999 UTC; el contexto multitemporal y su preparación consumen el comienzo del archivo. No existen seis años completos de señales evaluables. Son 87971 decisiones evaluables y 9548 señales aceptadas, con umbral original81 y noticias históricas ausentes neutrales. Sólo XRP30m: no extrapolar a los demás activos ni al móvil.

Se reutilizan las decisiones as-of del adaptador Kotlin y sus rangos, y las etiquetas de ForecastCriterion.kt. El adaptador comprueba paridad con el motor original en datos completos y documenta la corrección de una fuga de disponibilidad futura en cooldown ante huecos. Los patrones históricos sólo están disponibles tras madurar su ventana. No se aplica un modelo entrenado en2023 a2020.

Los candados comprueban que el predictor sigue siendo f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7 y el criterio a966a47244c2a99a7bf5b1d433fc7f4ab9c627a39e8f59537682864c2be7dd7b. Si cambian, se rechaza reutilizar decisiones o etiquetas.

## Resultado total descriptivo

9538 señales resueltas; 10 pendientes (huecos o ventana sin madurar). Porcentajes sobre resueltas, sin convertir pendientes en fallos:

| Resultado MAGNITUDE_TIME_V1 | Casos | Porcentaje |
|---|---:|---:|
| HIT exacto: objetivo completo en24h |7206|75,55%|
| APPROXIMATE: al menos75% en48h |975|10,22%|
| LATE: ese mínimo entre48h y120h |483|5,06%|
| FAIL: no alcanza el mínimo en120h |874|9,16%|
| Exacto o aproximado |8181|85,77%|

LATE no es acierto. Objetivo = punto medio del rango original. No son etiquetas del criterio operativo de primer toque, ni una simulación de ejecución o beneficio con costes.

## Desglose por año de emisión

| Año | Señales | Resueltas | Exactos | Aproximados | Tardíos | Fallos | Pendientes | Exactos% | Exactos+aproximados% |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
|2020|0|0|0|0|0|0|0|No evaluable|No evaluable|
|2021 parcial|585|583|458|58|28|39|2|78,56%|88,51%|
|2022|1901|1901|1392|218|106|185|0|73,22%|84,69%|
|2023|1772|1765|1273|208|130|154|7|72,12%|83,91%|
|2024|1950|1950|1487|178|102|183|0|76,26%|85,38%|
|2025|1883|1883|1453|178|63|189|0|77,16%|86,62%|
|2026 hasta02/10|1457|1456|1143|135|54|124|1|78,50%|87,77%|

## Dirección y dependencia

BUY: 3801 resueltas, 2785 exactas (73,27%), 422 aproximadas; exacto+aproximado84,37%. Diez pendientes.

SELL: 5737 resueltas, 4421 exactas (77,06%), 553 aproximadas; exacto+aproximado86,70%. Sin pendientes.

Agrupando transitivamente ventanas solapadas de120h por dirección y escogiendo la primera sin consultar el resultado: 123 episodios,94 exactos,10 aproximados,5 tardíos,14 fallos; 76,42% exactos y84,55% exactos+aproximados. Agrupar no demuestra independencia estadística.

El tramo llamado EXAMINATION en el candidato separado (desde02/10/2023) contiene5735 señales originales,5734 resueltas:4415 exactas,537 aproximadas,247 tardías,535 fallos y1 pendiente;76,997% exactos,86,362% exactos+aproximados. No confundir ese86,36% con acierto exacto ni con una nueva reserva no inspeccionada.

## Conclusión

El archivo histórico disponible no demuestra90% de acierto exacto. Tampoco llega al90% al sumar aproximados en este agregado. No demuestra una mejora respecto a otro motor: se ha auditado el original sin modificarlo. Todos los años y ambos sentidos quedan visibles; no se elige retrospectivamente el mejor año o umbral.

Los huecos siguen declarados en manifiestos, con reparaciones oficiales auditables, sin precios inventados. La auditoría sólo verifica procedencia de datos ya archivados, no vuelve a descargar todos los precios. Los resultados completos, hashes de entrada y ledger por señal se guardan en el artefacto de la nueva ejecución; report.json se archiva también en esta rama. Ningún cambio en producción, alarmas, persistencia o modelos prospectivos.
