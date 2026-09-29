# Pruebas V3.5.0 — puntos 26, 27, 29, 53 y 54

## Preparación

1. Abrir la carpeta raíz del proyecto en Android Studio con el JDK integrado 17.
2. Ejecutar `File > Sync Project with Gradle Files` y después `Build > Clean Project` y `Build > Assemble Project`.
3. Instalar sobre la versión anterior para comprobar también la migración SQLite V6→V7.

## Punto 26 — resultado y excursiones

1. Generar una señal y dejar que alcance uno o varios horizontes 15m/1h/4h/24h.
2. Abrir su ficha y comprobar que cada revisión muestra el precio, resultado y la hora de la vela de mercado usada.
3. Verificar que MFE usa máximos y MAE mínimos (invertidos correctamente para SELL) y que no se amplían después de 24 horas.
4. Ejecutar `OutcomeTrackerTest`: cubre BUY, SELL, selección temporal y ambigüedad intravela.

## Puntos 27 y 29 — aprendizaje y rendimiento fiable

1. Actualizar sobre una base con señales antiguas: deben seguir visibles en historial como `histórico heredado`.
2. Abrir Rendimiento: debe indicar cuántos registros heredados se excluyen.
3. Confirmar que total, cripto, timeframe y probabilidad comparable sólo cambian con señales V3.5 marcadas `EXACT_5M`.
4. Hasta reunir 10 resultados exactos comparables, la probabilidad debe continuar como “sin calibrar”.

## Punto 53 — exportación grande

1. Abrir `Exportación global` y probar TXT, CSV y RTF. Durante la generación se muestra estado y la interfaz no debe congelarse.
2. Abrir el CSV en Excel y el RTF en Word; comprobar configuración, análisis, señales, snapshots, seguimientos, alertas, alarmas y diagnóstico.
3. Probar Compartir: debe adjuntar un TXT mediante el selector Android, sin exponer una ruta de archivo.
4. Probar portapapeles. Si el informe supera 750.000 caracteres, la app debe pedir guardarlo o compartirlo en vez de agotar memoria.

## Punto 54 — backtest temporal

1. Ejecutar un backtest con series MTF suficientes.
2. Confirmar que no usa velas posteriores al instante evaluado, aplica 24 horas reales según el timeframe y separa BUY/SELL.
3. Un objetivo tocado antes que el stop debe ser HIT aunque el cierre final sea adverso; si ambos se tocan en la misma vela, debe ser NEUTRAL.
4. Confirmar que señales repetidas dentro del cooldown sólo entran cuando mejoran la confianza en más de 5 puntos.

## Regresión

- Verificar ficha individual y su exportación completa.
- Ejecutar `SignalEngineTest`, `RegimeClassificationTest`, `TechnicalEngineIncrementalTest`, `BacktestEngineTest` y `OutcomeTrackerTest`.
- Confirmar que AndroidX sigue activo en `gradle.properties` y que no se requiere `google-services.json` para el modo local.
