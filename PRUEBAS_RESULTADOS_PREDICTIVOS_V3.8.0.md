# Pruebas de resultados predictivos V3.8.0

## Objetivo

Comprobar que la aplicación separa el resultado de una operación de la persistencia del movimiento y que ambos se calculan sin información anterior a la señal.

## Pruebas automáticas incluidas

1. Primer toque de objetivo antes que stop.
2. Primer toque de stop antes que objetivo.
3. Objetivo y stop dentro de la misma vela de 5 minutos como resultado ambiguo NEUTRAL.
4. Ausencia de toque mantenida como pendiente hasta terminar la ventana.
5. Horizontes adaptados para 1h, 4h y 1d.
6. Objetivo y stop simétricos para BUY y SELL.
7. Rechazo de una compra tardía con RSI agotado y sin ruptura.
8. BTC coincidente sin bonificación y BTC contrario con penalización.
9. Backtest con ventana operativa adaptada.

## Prueba en el móvil

1. Instalar la APK sobre la versión 3.7.5 sin borrar datos.
2. Abrir HISTORIAL y verificar que las señales anteriores muestran `clasificación anterior` mientras se reclasifican.
3. Mantener vigilancia activa y conexión estable. La primera reconstrucción puede descargar varias páginas de velas de Binance.
4. Exportar el informe global y buscar estas secciones:
   - `SEÑALES`: debe incluir `status`, `persistence_status`, `legacy_status`, `outcome_reason`, `target_price` y `stop_price`.
   - `PERSISTENCIA ADAPTADA V2`: debe contener cuatro horizontes por señal.
   - `SEGUIMIENTOS ANTERIORES V1 CONSERVADOS`: debe conservar los registros antiguos.
5. En una señal resuelta, comprobar que `status` coincide con `TARGET_FIRST`, `STOP_FIRST`, `BOTH_TOUCHED_SAME_5M_CANDLE` o `NO_TOUCH_BY_DEADLINE`.
6. Comprobar que RENDIMIENTO cambia únicamente cuando termina el resultado operativo, no por la mayoría de cierres.

## Horizontes esperados

- 15m: 15m, 1h, 4h y 24h.
- 30m: 30m, 2h, 8h y 24h.
- 1h: 1h, 4h, 12h y 24h.
- 4h: 4h, 12h, 24h y 3 días.
- 1d: 1 día, 3 días, 7 días y 14 días.

## Criterio de aceptación

La vigilancia, las alertas y las alarmas deben continuar funcionando. Ninguna señal V2 resuelta puede aprender de la persistencia ni de la clasificación anterior; sólo debe usar el resultado operativo por primer toque.
