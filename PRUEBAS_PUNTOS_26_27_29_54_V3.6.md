# Pruebas V3.6.0 — puntos 26, 27, 29 y 54

## 1. Compilación y migración

1. Abrir la carpeta raíz en Android Studio con Embedded JDK 17.
2. Ejecutar Sync Project with Gradle Files y Build > Assemble Project.
3. Actualizar sobre V3.5 sin desinstalar y confirmar que Historial conserva las señales anteriores.
4. Abrir Rendimiento: las señales V3.5 deben figurar como heredadas excluidas, no como muestra exacta V3.6.

## 2. Seguimiento temporal exacto

1. Generar una nueva señal cerrada y anotar su `candle_close` en la exportación.
2. Confirmar que los vencimientos 15m/1h/4h/24h parten de ese cierre, aunque Android ejecute el ciclo más tarde.
3. Verificar que `market_ts` nunca es posterior al vencimiento y que MFE/MAE sólo usa velas cuyo inicio sea posterior al cierre de señal.
4. Reiniciar el teléfono entre dos horizontes y comprobar que los siguientes se recuperan sin duplicarse.

## 3. Aprendizaje y Rendimiento

1. Confirmar que la probabilidad permanece «sin calibrar» con menos de 10 resultados comparables exactos y resueltos.
2. Abrir Rendimiento y comprobar secciones: total, criptomoneda, intervalo, dirección BUY/SELL, régimen y riesgo.
3. Verificar que la suma BUY + SELL coincide con el total exacto y que pendientes no incrementan aciertos o fallos.

## 4. Backtest walk-forward

1. Ejecutar el backtest de una cripto distinta de BTC y confirmar que informa ventanas con contexto BTC.
2. Revisar los bloques total, BUY y SELL y la nota de contexto.
3. Repetirlo con BUY o SELL desactivado para esa cripto; no debe crear señales del lado desactivado.
4. Confirmar que los casos walk-forward empiezan en cero y sólo aparecen después de que las señales simuladas anteriores hayan completado 24 horas.
5. Si no se suministra una cronología de noticias históricas, debe indicarlo expresamente; no debe usar titulares actuales para fechas pasadas.

## 5. Pruebas unitarias

Ejecutar `testDebugUnitTest`. Deben pasar, entre otras, la exclusión de la vela que comenzó antes de la señal, el primer toque, la neutralidad de una vela ambigua y el contexto BTC histórico.
