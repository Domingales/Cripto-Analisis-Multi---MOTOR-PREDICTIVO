# Antecedentes de movimientos importantes: protocolo previo

José Domingo solicita revisar si hay una condición que se cumpla siempre antes de un movimiento. Primera faseXRP30m, mismo archivo2020–02/10/2026, contexto anterior y ventanas comunes desde2021. Cortes desarrollo/validación/examen iguales: desarrollo120h maduro antes01/04/2023, validación posterior madura antes29/10/2023, examen desde29/10/2023. No entrenar un predictor ni cambiar producción. Datos anteriores cerrados para condiciones, posteriores sólo para etiquetas.

NUEVO evento, separado de MAGNITUDE_TIME_V1:primario4%adicional desde el cierre de emisión en24h;secundario8%en72h. Subidas y bajadas separadas, ambas pueden ocurrir. No cambiar etiquetas antiguas ni presentar cifras de este evento como mejora de precisión del motor. No optimizar magnitud/plazo después de resultados.

Todas las ventanas evaluables archivadas; precios oficiales5m resuelven extremos. Exigir horizonte completo y continuo; huecos/corte dan PENDING aunque se vea un toque parcial. No rellenar. Ventanas solapadas no son movimientos independientes. Primeras ventanas espaciadas por horizonte,dirección y fase como sensibilidad; no garantía de independencia.

Diez condiciones fijadas,sin búsqueda de umbrales:volumen vela30m>=1,5media20anteriores;ADX>=25;squeeze anchoBollinger20/2percentil20de180anchosanteriores;ruptura cierre sobre máximos/bajo mínimos20velasanteriores;ATR14SMA actual>=1,2ATR14SMA siete velas antes;ADX>=20y mayor que90minantes con snapshots intermedios presentes;MTF>=8alineado;BTCretorno última4hcerrada alineado;retorno4velas30malineado;cierre en20%extremo de su vela y volumen>=1,5. No sustituir ATRSMApor Wilder/ATRAndroid ni afirmar equivalencia. Ruptura ya puede significar inicio de movimiento; se mide movimiento adicional futuro, NO predicción infalible de su inicio.

Por condición y evento:tablaTP/FP/FN/TN, precisiónP(movimiento|condición),coberturaP(condición|movimiento),frecuencia base,lift,desconocidos y hasta3contraejemplos positivos sin condición. Condición necesaria no equivale a suficiente. Una ausencia comprobada refuta «siempre» en esa muestra; ausencia de contraejemplos NO demuestra ley universal. No elegir condiciones por examen ni activar reglas. Publicar10condiciones y2eventos completos.

Fuentesprimarias revisadas29/10/2026:
-Liu/Tsyvinski,NBER24877(2018), https://www.nber.org/papers/w24877 :momentum y atención,sin garantía universal.
-Schmeling/Schrimpf/Todorov,BIS1087(2023), https://www.bis.org/publications/working-paper-1087-crypto-carry :carry/apalancamiento y riesgo de desplomes;BTC/ETH,no todos los mercados.
-Cont/Kukanov/Stoikov(2010/2014), https://arxiv.org/abs/1011.6402 :desequilibrio de órdenes y profundidad,estudioacciones,no prueba directaXRP.
-JohnBollinger, https://www.bollingerbands.com/bollinger-band-rules :tocar banda no es señal suficiente.

El archivoOHLCV no contiene libroL2histórico,órdenes canceladas,carry/funding/OI ni atención histórica. No reconstruirlos desdevelas ni titularesactuales. Pueden motivar investigaciones separadas si se consiguen datos verificables disponibles entonces. Ninguna fuente prueba100%de aciertos. No operaciones financieras ni rentabilidad en esta revisión.
