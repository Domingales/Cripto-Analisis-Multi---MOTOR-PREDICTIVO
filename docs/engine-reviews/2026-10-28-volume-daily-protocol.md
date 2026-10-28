# Experimento diario precio/volumen V1 — 28/10/2026
Reglas fijadas antes de consultar resultados. Investigación aislada, PR9/research/expanded-replay-150. No producción ni contratos retroactivos prospectivos.

## Alcance cerrado
BTC, ETH y XRP, intervalo1d UTC. Preparación oficial desde2020-10-02 cuando exista; examen fijo2023-10-02–2026-10-02T15Z. No repetir matriz150. Historial ya inspeccionado en otros experimentos: resultados exploratorios, no reserva independiente.
No optimizar periodos/umbrales por resultados. Dos hipótesis por tres activos=seis contrastes primarios; no proclamar ganador por máximo bruto.

## Dos firmas BUY
UD50: sumar volumen de las50últimas sesiones, incluida la actual, cuyo cierre supera el cierre previo; dividir por volumen de las sesiones cuyo cierre es inferior al previo. Empates excluidos. Señal sólo si denominador>0 y ratio>1. Denominador0 o huecos: no evaluable, no infinito ni éxito.
PP10_VOLUME: sesión actual alcista (cierre>cierreprevio), cuyo volumen supera ESTRICTAMENTE el máximo volumen de las sesiones bajistas de las10sesiones PREVIAS, excluida la actual. Sin sesiones bajistas previas con volumen positivo:no evaluable. Es sólo firma de volumen, NO método completo pocket pivot con fundamentales/medias/base.
No introducir ahora contracción, breakout o filtros adicionales no definidos.

No identificar fondos ni llamar entrada neta de capital al ratio. Referencias de origen:
https://www.virtueofselfishinvesting.com/faqs/answer/ten-rules-for-pocket-pivots
https://doi.org/10.1093/rfs/hhm079
El segundo estudio trata atención inversora; no prueba estas reglas.

## Cronología y comparación
Sólo velas cerradas disponibles en cada fecha; control por alteración de velas futuras. Guardar TODAS las decisiones/rechazos antes de leer etiquetas de resultados. Reconstruir rangos originales con el adaptador auditado.
Entrada de referencia=cierre diario conocido; objetivo común=punto medio REAL forecastLowPct/highPct del original. Etiquetar ambas direcciones con ForecastCriterion.kt intacto:HIT24h,APPROXIMATE75%48h,LATEhasta120h,FAILtras120h; huecos pendientes.
Comparar cada regla BUY con BUY sin filtro en las mismas oportunidades y con señales BUY aceptadas del original; original por todas las direcciones se informa aparte. Filtros sobre señales BUY originales son secundarios descriptivos, no nuevos ganadores.
Cobertura, no evaluables, por año, régimen al decidir, tamaño del objetivo, MFE/MAE y primera señal de cada unión TRANSITIVA120h. No afirmar independencia por no solapar. No probabilidad90% certificada ni Brier para reglas que no emiten probabilidades.

## Ejecución histórica separada
Simulación hipotética BUY/SELL:entrada a apertura de primera5m posterior al registro simulado diario; niveles objetivo/stop simétricos anclados al precio de referencia y magnitud común, no stop original de la app. Máximo120h.
Si apertura fuera de ambos niveles:no ejecutable. Huecos:pendiente; apertura que salta stop:sale al precio de apertura; salto favorable:beneficio limitado al objetivo; ambos niveles en una5m:intervalo ambiguo, no elegir orden favorable.
Fee0,10%por lado; deslizamiento0/0,10/0,20%por lado. Supuestos ilustrativos, no tarifas verificadas. Admisión única por activo/estrategia hasta fin de ventana120h, incluso si habría salida antes; reglas de admisión usan sólo tiempos, nunca resultados. Sin apalancamiento; SELL hipotético sin financiación/préstamo.
Publicar retornos por caso, no sumar operaciones solapadas ni inferir cartera. No crear recibos de durabilidad ficticios:cohorte histórica distinta.

## Protección
No reentrenar ni cambiar modelos/protocolos/umbrales de cohortes activas. No modificar siete archivos del predictor ni candados. Tests Python, paridad/no-futuro del adaptador, etiquetas Kotlin y compilación APK en Actions. Informes persistentes en GitHub y artefactos reproducibles.
