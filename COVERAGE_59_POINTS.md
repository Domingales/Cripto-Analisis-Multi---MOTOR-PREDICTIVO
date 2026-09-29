# CriptoAnálisis Multi 3.8.0 — verificación de los 59 puntos

La V3 cambia el criterio de entrega: una capacidad no debe quedar sólo en el motor; debe ser localizable desde la APK o auditable en diagnóstico/base de datos.

## Evidencia visible principal

- **Dashboard**: puntos 1–2, 17–24, 29, 34–37, 41–43, 55 y 57; incluye variación, ordenación, última alerta, alarmas y seguimiento.
- **Mis Criptos + Configurar cripto**: puntos 3, 4 y 58.
- **Ficha completa de cripto**: puntos 7–20, 27–29, 38–39.
- **Seguimiento dentro de ficha / Historial**: puntos 24–28 y 48.
- **Rendimiento**: punto 29, con desglose global, cripto, timeframe, BUY/SELL, régimen y riesgo.
- **Diagnóstico**: puntos 21–23, 31–35, 42–43, 45–48, 51 y 57.
- **Configuración**: puntos 4, 33, 34, 40, 47 y 50; incluye sonido/vibración global y por activo, prueba de canal y lado del menú.
- **Alarmas de precio**: punto 1; condición, objetivo, duración sostenida, observación y ciclo de estados.
- **Backtest**: punto 54.
- **Exportación**: punto 53; incluye exportación global completa en portapapeles, compartir, TXT, CSV/Excel y RTF/Word, además de la ficha individual completa.
- **Mapa de los 59 puntos**: índice visible de todo el diseño.

## Cobertura funcional

1. Selector/intervalos/análisis/alertas de señal y precio/historial/gráfico/exportación accesibles desde el menú.
2. Central multcripto independiente por activo.
3. Catálogo dinámico Spot USDT, buscador, selección múltiple y favoritos persistentes.
4. Configuración por cripto con sonido y vibración independientes.
5. Repositorio de datos centralizado.
6. OHLCV histórico + precio actual separado.
7. EMA20/50/200 y pendientes.
8. RSI, pendiente y divergencias.
9. ADX/+DI/-DI.
10. ATR/ATR%/riesgo/rango esperado.
11. Ratio y expansión de volumen.
12. Soportes/resistencias por zonas.
13. Estructura de mercado.
14. Análisis multitemporal.
15. Régimen de mercado con estado MIXED neutral y giro sólo con evidencia.
16. Contexto BTC 4h.
17. Indicador Maestro desglosado.
18. Score, confianza y probabilidad separados.
19. BUY/SELL/WAIT/giros.
20. Predicción probabilística, rango e invalidación.
21. Cadena de señal auditable.
22. Señal, snapshot, persistencia adaptada, evento y outbox persistidos transaccionalmente antes de notificar.
23. UID único.
24. Historial por símbolo/timeframe.
25. Snapshot exacto de indicadores, rupturas, score, aprendizaje, noticias y configuración + persistencia adaptada al timeframe.
26. Resultado operativo PENDING/HIT/FAIL/NEUTRAL por primer toque, persistencia separada y MFE/MAE OHLC con velas 5m posteriores.
27. Casos comparables `EXACT_FIRST_TOUCH_5M_V2` del mismo lado BUY/SELL, con indicadores, estructura, S/R, MTF y contexto; persistencia y heredados quedan excluidos.
28. Niveles de confianza estadística; probabilidad oculta hasta 10 resultados comparables resueltos.
29. Autoevaluación exacta total/cripto/timeframe/dirección/régimen/riesgo, con heredados excluidos identificados por separado.
30. ForegroundService `specialUse` coordinador permanente, independiente de la interfaz y compartido por ejecución local, remota y manual.
31. Ciclos locales con el intervalo configurado, FCM directo y JobScheduler de respaldo; notificaciones disponibles con pantalla apagada dentro de las restricciones reales del sistema.
32. BOOT_COMPLETED y MY_PACKAGE_REPLACED restauran el coordinador, el respaldo y el registro remoto si la vigilancia estaba activa.
33. Cuatro canales de señal para las combinaciones sonido/vibración, prueba visible y contenido completo de la alerta.
34. UID + cooldown + mejora mínima.
35. Tendencia/ruptura/giro/volumen en régimen/recomendación.
36. Dashboard operativo con variación, ordenación persistente, última alerta, alarmas activas y seguimientos.
37. Semáforo visual con “EN SEGUIMIENTO” morado y estados diferenciados.
38. Ficha individual con gráfico, EMA20/50/200, pendientes, RSI, ADX/DMI, ATR, volumen y zonas S/R.
39. Explicación y rechazo visibles.
40. Menú hamburguesa global completo, configurable a izquierda o derecha.
41. Radar de oportunidades por prioridad.
42. Diagnóstico de red, API, latido del servicio, optimización de batería, origen/retraso del ciclo y salud/error por criptomoneda.
43. Registro de no-señal.
44. Persistencia estructurada V10 de activos/configuración/velas/análisis/snapshots/señales/resultado operativo/persistencia/patrones/alertas/outbox/diagnóstico.
45. Coordinador serial, concesión persistente antisolapamiento y pool limitado.
46. Caché persistente sincronizada; EMA/RSI incrementales por nueva vela y reutilización por cierre de vela.
47. Evaluación dual: vela abierta sólo PRESEÑAL y última vela cerrada apta para SIGNAL en el mismo ciclo.
48. EVALUATION/PRESIGNAL/SIGNAL/ALERT/RESULT.
49. Decisión reproducible y local.
50. Noticias en módulo separado, peso auxiliar.
51. Timeouts/retry/backoff/validación/frescura.
52. Migraciones consecutivas V1→V10 y continuidad de datos, incluida conservación de la clasificación anterior.
53. Exportación global y por ficha: portapapeles, compartir, TXT, CSV/Excel y RTF/Word; generación global por flujo en segundo plano.
54. Backtest walk-forward MTF sin look-ahead, contexto BTC histórico, aprendizaje sólo tras resolverse, ventana adaptada, primer toque, cooldown y BUY/SELL.
55. WAIT/no operar sin forzar cantidad de señales.
56. Capas data/domain/service/ui/util.
57. Flujo multcripto completo ejecutable con interfaz cerrada mediante servicio permanente y respaldo remoto/local.
58. Sin cripto global única.
59. Proyecto nuevo CriptoAnálisis Multi V3.8.0.

## Decisiones de implementación

El proyecto mantiene `SQLiteOpenHelper` para la base transaccional existente y un canal rápido REST para precio actual. Son decisiones de implementación que no alteran la semántica funcional de los 59 puntos. La arquitectura permite sustituir el almacenamiento por Room o el ticker por WebSocket sin modificar el motor de decisión.
