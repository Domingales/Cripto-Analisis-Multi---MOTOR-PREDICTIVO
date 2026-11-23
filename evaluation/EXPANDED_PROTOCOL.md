# Ampliación autorizada — 4 octubre 2026

## Alcance
Catálogo actual Assets.kt: 30 activos × 5 intervalos (15m,30m,1h,4h,1d) = 150 combinaciones. BTC forma parte del examen y también aporta contexto a otros activos. Velas de 5m sirven para resolución y agregación, no son un sexto intervalo predictivo de la app.

Primera fase implementada: recogida verificable desde 2020-10-02 hasta el corte reproducible 2026-10-02 15:00 UTC, igual al ensayo original. Los meses inexistentes, activos retirados, cambios de símbolo y huecos permanecen explícitos; no sustituir automáticamente MKR u otros activos por sucesores. Una descarga terminada no significa histórico completo ni prueba predictiva terminada.

Los intervalos se agregan de OHLCV oficial 5m con buckets UTC completos. Se conserva hash del origen, CHECKSUM de archivos oficiales y manifest de cada serie. Una vela incompleta o con huecos no se genera. Dos descargas simultáneas como máximo. Conservar informes de fallos incluso sin datos.

## Comparación pendiente tras auditar la cobertura
Adaptar el evaluador Kotlin y el alternativo sin cambiar código de producción. El ensayo inicial fijo permanece intacto y reproducible. Revisar cobertura y probar paridad de agregación con velas oficiales de otros intervalos antes de usar estas series para pronosticar.

Horizontes de producción: 15m/30m/1h = 24h; 4h = 72h; diario = 14 días. Embargo y etiquetas deben utilizar ese horizonte, nunca 24h indiscriminadamente. Motor original: umbral81, mismos filtros/cooldown y contextos de producción. Alternativo: variables sólo de mercado, modelo y calibración congelados antes del examen, umbral65 inicial sin ajustes posteriores.

Entrenamiento anterior a 2023-04-01, calibración posterior pero anterior a 2023-10-02 con exclusión de etiquetas que crucen fronteras; examen desde 2023-10-02 donde haya datos suficientes. Cuando un activo no tenga preparación suficiente, declarar NO EVALUABLE en el ensayo fijo; diseñar posteriormente un inicio desplazado documentado y comparable. No entrenarlo con datos futuros para completar artificialmente las150 combinaciones.

Guardar decisiones antes de revelar resultados, excluir contexto no cerrado o con huecos. Informes por combinación, año, dirección y régimen, con señales/no operar, aciertos/fallos/neutros/pendientes y cobertura real. Noticias históricas ausentes neutrales. No interpretar horizontes solapados como muestras independientes. Ajustar inferencia por múltiples comparaciones; no escoger ganador sólo por mayor porcentaje. Datos ya inspeccionados no son validación intacta.

Simulación de ejecución/costes sigue pendiente; sensibilidad a barreras no equivale a rentabilidad ejecutable. Integrar sólo ventajas verificadas posteriormente con datos nuevos. Ningún cambio en APK, alertas, Firebase, Cloudflare ni persistencia.

## Estado
PREPARACIÓN Y RECOGIDA. Comparación de las150 combinaciones todavía NO ejecutada. Sin mejora predictiva nueva ni motor ganador.
