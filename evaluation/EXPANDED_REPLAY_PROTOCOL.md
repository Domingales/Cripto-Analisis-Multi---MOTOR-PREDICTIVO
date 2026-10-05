# Comparación cronológica ampliada — 5 octubre 2026

Autorización: continuar las 30 criptomonedas y los cinco intervalos del catálogo,
150 combinaciones. Documento original de los 59 puntos leído en esta sesión.
Trabajo fuera de producción: sólo herramientas de evaluación, pruebas y CI.

## Fechas y condiciones congeladas

Histórico desde 2020-10-02 donde exista. Examen desde 2023-10-02 hasta el corte
reproducible 2026-10-02 15:00 UTC. Conservamos ese corte auditado; el tramo posterior
no forma parte de este examen. Datos Actions 37224269151. Paridad oficial Actions
37275087288: 295 muestras coinciden; cinco muestras MKR de septiembre de 2026 no
tienen histórico derivado porque el símbolo terminó en septiembre de 2025.
Son muestras de contraste, no una comprobación independiente de cada vela.

Motor original: BacktestEngine sin cambios matemáticos, umbral81, cooldown60min,
calibración sólo con resultados disponibles a la fecha simulada. El adaptador
generado en fuentes de TEST sustituye búsquedas lineales por binarias y memoriza
el indicador de una vela cerrada. Se exige paridad de análisis, aceptación y
resultados con el motor intacto antes de iniciar los trabajos del histórico.
La prueba adicional altera velas posteriores y compara decisiones anteriores.

MTF: conjunto único 15m/intervalo principal/4h/1d, contexto BTC4h salvo BTC.
Exigir continuidad de hasta300 velas cerradas, con mínimo210, y última vela
esperada según el reloj UTC. Noticias ausentes, neutrales. El ensayo antiguo
ADA1h no tenía contexto15m: los porcentajes nuevos no son directamente una
mejora sobre aquel51,77%. El backtest calcula ventanas300; no replica toda la
caché incremental del móvil ni ejecuta su interfaz o sus alarmas.

Alternativo: variables sólo de mercado, cuatro familias del ensayo previo
(rendimientos, volatilidad/rango, volumen, distancia de medias), ampliadas al
mismo MTF y BTC. Entrenamiento con etiquetas terminadas antes de2023-04-01.
Calibración antes de2023-10-02 menos un horizonte adicional. Modelo congelado,
umbral65%, sin ajustes tras observar el examen. Mínimo100 etiquetas resueltas
y ambas clases por dirección en entrenamiento y calibración; de lo contrario
NO EVALUABLE, no un supuesto modelo que "no encontró señales".

Horizontes:15m/30m/1h=24h;4h=72h;1d=14d. Resolución con todas las velas5m del
horizonte, primer toque en barreras simétricas de max(0,45%,0,55ATR%). Si ambas
barreras se tocan en la misma vela:neutral. Hueco:pendiente explícito. Horizonte
sin terminar:pendiente. Diario no se resuelve en24h.

Decisiones duraderas antes de revelar resultados del examen. Guardar versión,
hashes de inputs y modelo, variables, dirección, rechazo y aceptación.
Cada combinación tiene informe por año, régimen y dirección; frecuencia,
acierto/fallo/neutral/pendiente, cobertura, sensibilidad de costes de barreras.
Los precios no se rellenan ni se sustituyen símbolos. MKR conserva su final real.

## Ejecución e interpretación

CI: verificar preparación y paridad, ejecutar150 trabajos con máximo4 simultáneos,
agregar150 informes únicos. Si faltan trabajos, el resumen se marca PARCIAL y
la comprobación falla. Conservar informes, decisiones y modelos90 días.
Una combinación sin modelo alternativo suficiente mantiene su resultado del
original y declara la comparación no disponible. No desplazar fechas usando
resultados posteriores para aparentar que las150 comparaciones eran evaluables.

Datos y código posteriores a2023: reconstrucción histórica exploratoria, no
validación prospectiva independiente. Horizontes solapados no son muestras
independientes; explorar150 combinaciones tampoco autoriza escoger ganador por
el mayor porcentaje. Las candidatas necesitarán control de múltiples pruebas,
incertidumbre por bloques temporales y validación con pronósticos nuevos.
Sensibilidad a costes no equivale a simulación de rentabilidad ejecutable.

Estado al preparar esta versión:6 pruebas específicas Python y29 anteriores
pasan localmente; compilación y paridad Kotlin pendientes de ejecución enCI.
No hay todavía resultados de las150 combinaciones ni mejora predictiva nueva.
