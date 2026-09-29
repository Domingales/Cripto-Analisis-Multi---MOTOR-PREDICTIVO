# Pruebas de aceptación — V3.4.0

## Preparación

1. Configura Android Studio con `Gradle JDK: Embedded JDK (17)`.
2. Ejecuta `Sync Project with Gradle Files` y `Build > Assemble Project`.
3. Para comprobar la migración, instala V3.4.0 encima de la versión anterior sin borrar sus datos.
4. Confirma que el historial antiguo continúa visible y que Diagnóstico no muestra errores SQLite.

## Punto 15 — régimen neutral correcto

1. Abre la ficha de un activo sin tendencia, ruptura, divergencia ni falsa ruptura.
2. El régimen debe ser `MIXED`, no `POSSIBLE_REVERSAL`.
3. Comprueba un caso con divergencia RSI o falsa ruptura: sólo entonces puede aparecer `POSSIBLE_REVERSAL`.
4. Ejecuta los tests `RegimeClassificationTest`.

## Punto 25 — snapshot exacto V6

1. Genera una señal nueva en vela cerrada y exporta su ficha.
2. Comprueba que el snapshot conserva precio/cierre, RSI y pendiente/divergencias, ADX/DMI, ATR, EMA y pendientes, volumen, zonas S/R con toques y fuerza, estructura, régimen, rupturas, MTF, BTC, los diez componentes, aprendizaje, noticias, rango, invalidación, umbral, lados permitidos y modo de vela.
3. Los snapshots anteriores deben conservarse con calidad `LEGACY_V4`; no deben presentarse como V6 exactos ni entrar en el aprendizaje comparable avanzado.

## Punto 27 — casos del lado definitivo

1. Comprueba que una evaluación técnicamente BUY solicita comparables BUY y una SELL solicita comparables SELL.
2. Si el histórico altera el lado resultante, el motor debe recalibrar con el nuevo lado; nunca debe aplicar una muestra BUY al desglose SELL ni viceversa.
3. La probabilidad debe seguir sin mostrarse mientras no existan al menos diez casos resueltos comparables.
4. Ejecuta `SignalEngineTest`; incluye la prueba de aislamiento de la componente histórica.

## Punto 47 — evaluación dual intravela

1. Activa el modo intravela y ejecuta un ciclo.
2. Deben guardarse dos evaluaciones: la última vela cerrada y la vela actual abierta.
3. La evaluación abierta puede quedar como `PRESIGNAL`, pero nunca como `SIGNAL` ni generar alerta.
4. La evaluación cerrada sí puede confirmar `SIGNAL` si supera los filtros.
5. Repite el ciclo durante la misma vela: la cerrada no debe duplicarse; la intravela puede actualizar el estado observable.

## Punto 53 — exportación completa

1. En `Exportación global`, prueba copiar al portapapeles, compartir, guardar TXT, guardar CSV para Excel y guardar Word/RTF.
2. Verifica que aparecen configuración, criptos, evaluaciones y rechazos, señales, snapshots exactos, seguimientos, eventos/outbox, alarmas de precio y diagnóstico.
3. Abre el CSV en Excel y comprueba las columnas `seccion`, `registro`, `campo` y `valor` en UTF-8 con separador `;`.
4. Abre el RTF con Word y confirma que los acentos y saltos de línea son correctos.
5. Desde cualquier ficha de criptomoneda, confirma que la exportación individual permite copiar, compartir y guardar TXT, CSV/Excel y RTF/Word.
