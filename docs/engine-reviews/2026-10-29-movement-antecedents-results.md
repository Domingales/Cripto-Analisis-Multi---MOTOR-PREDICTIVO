# Antecedentes de movimientos importantes: revisión histórica y fuentes29/10/2026

## Resultado

Ninguna de las10condiciones fijas se cumple siempre antes de los eventos estudiados. Hay contraejemplos para las10en BUY/SELL y en4%24h/8%72h:40relaciones condición-evento refutadas como universales EN ESTA MUESTRA. No prueba que se hayan explorado todas las condiciones posibles ni todos los activos.

Primera faseXRP30m, archivo oficial02/10/2020–02/10/2026, contexto y ventanas comunes desde14/06/2021;desarrollo anterior01/04/2023,validación hasta29/10/2023 con purga120h,examen posterior. Fuentes y umbrales fijados en protocolo22015fee31028e31f88f342a3c0a051ef63da855 antes de resultados,códigoacde2d6b39375085abc6a6566bf801554feb653c. Run40960072157 SUCCESS.

Este es un estudio NUEVO de eventos fijos:4%adicional desde el cierre de emisión durante24h,y8%en72h. NO son etiquetas MAGNITUDE_TIME_V1 ni porcentajes del predictor original. Se valora el precio máximo/mínimo posterior en5m, no ejecución con costes ni final de día. Ambas direcciones pueden alcanzar objetivo. Una condición conocida al emitir precede el objetivo futuro, pero una ruptura ya puede llegar cuando el movimiento comenzó. No afirmar predicción del inicio.

## Diferencia entre condición necesaria y señal fiable

En examen hay51342ventanas:51294completas para24h y48pendientes por huecos/corte.11296ventanas alcanzan+4%y11938alcanzan-4%;se solapan y no son movimientos independientes.

| Condición previa,para subidas4%24h | Presente en ventanas positivas | Veces que hay+4%cuando aparece condición | Base+4%sin filtrar |
|---|---:|---:|---:|
| Volumen>=1,5media20previas |19,53%|24,90%|22,02%|
| ADX>=25 |74,06%|22,57%|22,02%|
| SqueezeBollinger20/2percentil20previo |18,37%|17,29%|22,02%|
| Ruptura cierre>máximo20anteriores |5,85%|27,97%|22,02%|
| ATR14SMAexpande>=20%respecto7velasantes |23,46%|25,17%|22,02%|
| ADX>=20y creciendo frente90minantes |45,47%|22,68%|22,02%|
| AlineaciónMTFalcista>=8 |26,90%|32,90%|22,02%|
| Último retornoBTC4hcerrado positivo |53,82%|23,19%|22,02%|
| Retorno4velas30mpositivo |52,28%|23,04%|22,02%|
| Cierre20%superior+volumen>=1,5 |4,36%|26,80%|22,02%|

No interpretar19,53%como acierto ni74,06%como precisión. Primera columna=P(condición|evento),segunda=P(evento|condición). Por ejemplo,ADXalto está presente en8366ventanas que suben4%,pero otras2930suben sin él;y28705ventanas conADXalto no suben4%. Alta frecuencia deADXnoes una señal fiable.

MTFalcista es la asociación descriptiva positiva más marcada de esta tabla:3039TP/6198FP,precisión32,90%,frente22,02%base,lift1,49. Sigue habiendo67,10%sin+4%en24h cuando aparece. No se elige como nuevo filtro ganador tras mirar el examen. En desarrollo su precisión era31,18%conbase27,54%;validación23,33%conbase15,94%. La magnitud y estabilidad del efecto varían.

Sensibilidad escogiendo primeras ventanas espaciadas>24h antes de consultar resultado:MTF58TP/128FP/176FN/685TN/1pendiente,31,18%precisión y24,79%cobertura;no prueba independencia ni certifica probabilidades.

Para bajadas4%24h,ADXalto cubre73,34%pero sólo23,62%de sus apariciones anteceden bajada4%(base23,27%). Ruptura bajista cubre4,73%y tiene25,80%precisión. Squeeze cubre19,78%y tiene19,68%precisión. Tampoco son condiciones universales.

Secundario8%72h:ninguna condición universal. EjemploMTFalcista tiene29,49%precisión y26,46%cobertura,frente20,05%base. Se mantienen todos los resultados de las10condiciones,sin seleccionar el porcentaje más bonito.

## Contraejemplo reproducible

30/10/2023 00:29:59.999UTC,cierreXRP0,5557USDT:en la ventana posterior completa se alcanza+4%sinADX>=25al emitir. Refuta la necesidad deADXalto para ese evento;no es una alerta registrada prospectivamente ni un caso independiente de ventanas vecinas. report.json contiene hasta3contraejemplos por condición,eventodirección y fase,con timestamps y entradas verificables.

## Qué dicen las fuentes primarias

Revisadas29/10/2026,paráfrasis,no reglas garantizadas:

1.Liu/Tsyvinski,NBER24877(2018), https://www.nber.org/papers/w24877 :documenta factores de momentum y atención enBTC/XRP/ETH. No demuestra una condición necesaria universal ni una probabilidad90/100%intradiaria.
2.Schmeling/Schrimpf/Todorov,BIS1087(2023), https://www.bis.org/publications/working-paper-1087-crypto-carry :relación de carry/apalancamiento con riesgo de desplomes enBTC/ETH. No equiparar carry a indicador infalible del próximo4%.
3.Cont/Kukanov/Stoikov(2010,publicado2014), https://arxiv.org/abs/1011.6402 :relación contemporánea entre desequilibrio de órdenes,profundidad e impacto de precios;estudio50accionesNYSE,no validación deXRP. Puede motivar variables distintas aOHLCV,pero no prueba anticipación universal.
4.JohnBollinger, https://www.bollingerbands.com/bollinger-band-rules :advierte que tocar una banda no basta como señal y que indicadores redundantes no suman evidencia independiente.
5.Bieganowski/Ślepaczuk,preprint2026, https://arxiv.org/abs/2602.00776 :resumen consultado,NOreplicado;explora patrones microestructurales enBTC/LTC/ETC/ENJ/ROSE,datos2022–2025. No se presenta como evidenciaXRP ni conocimiento disponible en2023. No garantía de rentabilidad o universalidad.
6.Binance,documentación primaria https://github.com/binance/binance-public-data :archivosklines originales incluyen volumencompradoragresor y operaciones;aggTrades incluyen marca de iniciador. Los7campos de nuestra extracción no conservan esos datos adicionales. Checksums y cambios de unidades(timestampsmicrosegundos desde2025)requieren control. No se han analizado esos campos en este estudio.

La hipótesis siguiente más distinta de volver a combinar indicadores derivados del cierre es estudiar flujo agresor y liquidez,si se recupera historia verificable. Es una inferencia de investigación,no un resultado probado. Volumen comprador agresor no es libroL2completo;OHLCVno permite reconstruir cancelaciones o profundidad. Carry/funding/OI/atención histórica tampoco se deducen develas ni se reemplazan connoticiasactuales. No contratar datos ni afirmar cobertura6años sin comprobarla.

## Integridad,pruebas y límites

164pruebasPython y Android testDebugUnitTest assembleDebug correctos. Archivo raw30mreconciliado/reparado sólo con fuentes oficiales auditadas,5mconhuecosconservados;hashes iguales a la línea de datos del replay. No precios rellenados ni candados relajados. Antecedentes guardados antes de leer extremos futuros;ningún umbral ajustado por examen.

Artefacto12935178562,SHA256941b112001742480e46ee2d77d2e1985173ff033a53d92f0cab01da12d65fb15,descargado y checksumverificado. Contiene antecedentes,tablas por desarrollo/validación/examen,sensibilidad espaciada,counterejemplos,manifiestos y pruebas. report.jsoncompleto y summary.jsondurables en esta rama.

Ventanas dependientes;no usar cientos de ventanas vecinas como cientos de pruebasindependientes. Histórico inspeccionado,asociaciónno causal,alcanceXRP30msólo,comienzo2021traspreparación,fincorte02/10/2026,nofechaactual. No ley universal ni certificación90/100%. Producción/modelos/umbrales/recogidasprospectivas intactos. No etiqueta de rentabilidad ni operaciones reales.
