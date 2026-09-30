# Evaluación del motor — 30 septiembre 2026

Base: 501819f916634486d2aeecfcdbe29a6d97d6d135. Requisitos originales 1–59 leídos desde el documento maestro. Sin cambios en SignalEngine, TechnicalEngine, alertas ni interfaz Android.

Hipótesis verificable: faltaba observación prospectiva y comparación reproducible de candidatas. Mejorar la instrumentación, manteniendo exactamente los resultados históricos, permite evaluar posteriormente cambios predictivos.

Referencia: corte 2026-09-29T00:00:00Z, 45 días ADAUSDT 1h/5m, ADA 4h/1d y BTC 4h. Umbral 81, 836 ventanas, 126 señales, HIT 67, FAIL 58, NEUTRAL 1. BUY 55/41/0 de 96; SELL 12/17/1 de 30. Cobertura 15,07%, precisión resuelta 53,6%. Último tramo: 29/25 de 54 (descriptivo, no reservado).

La referencia se conserva íntegra en reference_501819f.zip: velas, manifiesto y casos originales del artefacto 11058780368, ejecución 36621164120. El comparador verifica igualdad byte a byte de todas las velas antes de comparar casos, señales añadidas/eliminadas y BUY/SELL. Reporta Wilson 95%; los horizontes solapados impiden interpretarlo como certeza bajo independencia.

La nueva ruta del backtest observa decisiones hasta la última vela cuando se solicita explícitamente. Los valores por defecto conservan la evaluación original; las señales no vencidas no alimentan la calibración.

Recogida prospectiva: cada cuatro horas, última vela cerrada; registro append-only con commit, contexto completo, datos disponibles, noticias ausentes/neutrales, precio, objetivo/stop simétricos, horizonte y reglas. No recupera retrospectivamente las decisiones perdidas. Es una cohorte retrasada de replay de 45 días y no reproduce el estado histórico del móvil. Para excluir cualquier movimiento anterior al registro, la ventana comienza en la siguiente vela 5m completa; esta cohorte debe analizarse por separado de la referencia histórica.

Al vencer 24h se requiere la serie 5m completa. Primer toque; ambos niveles en la misma vela son ambiguos/neutrales. Huecos: pendiente. El registro sobre main sólo se añade mediante push normal, sin force ni sobrescribir cambios ajenos; un conflicto bloquea esa escritura y conserva el artefacto para recuperación.

Criterio de publicación de esta instrumentación: pruebas Android y compilación exitosas; referencia idéntica caso por caso; captura prospectiva válida en la PR. No afirmar mejora de precisión porque las reglas no cambian. Las futuras candidatas predictivas requieren hipótesis, comparación con las mismas velas y evidencia reservada/prospectiva suficiente, incertidumbre, cobertura y frecuencia.

Próxima revisión: inspeccionar Actions y events.jsonl, evaluar casos vencidos y separar BUY/SELL, pendientes y ausencia de señales. No optimizar a diario las mismas 126 señales. Si una ejecución tarda más de 20 minutos desde la última vela, rechazar la captura antigua en vez de presentarla como prospectiva.
