# Revisión 2026-10-07: discrepancia oficial SOL y registro prospectivo

Revisión a las 08:26 Europe/Madrid. Investigación; sin cambios al motor Android, umbrales o criterios de evaluación. Documento original de 59 puntos leído íntegramente desde libfile_e43bbafca5108191a8290427f667cef0 (DOCX materializado y texto extraído; el lector de Library no produjo texto).

## Estado fresco de GitHub

- main consultado: ee800503c61443ec18f73e75b76f1ed3322a1733; últimos cambios son registros prospectivos, no fusión de PR9.
- PR9 abierto, no integrado; head d606298c1b4dc159ae3cfc1dee0dd74d39fbadf7.
- 37479151721: failure en reparación real; 51 tests Python OK; matriz150 y resumen omitidos. No hay repetición completa nueva.
- Android37479151477 y auditoría37479151530: success.
- Captura15 37479151465: success; log comprobado AWAITING_REGISTERED_START, 0 decisiones/0 outcomes. Ejecutada 2026-10-06 antes del inicio; persistencia omitida. No contar pruebas como registros futuros.
- Registro main37572957638: success, terminó 2026-10-07T04:46:34Z. Ledger actual:28 DECISION,2 aceptadas,1 OUTCOME HIT. La segunda señal aceptada aún no tiene OUTCOME; ventana hasta1791371099999 (2026-10-07T11:04:59.999Z). Frente a24 decisiones comunicadas el6 por la mañana:4 decisiones nuevas,1 señal nueva. Un acierto entre un caso resuelto no acredita fiabilidad.

## Hallazgo nuevo comprobado directamente con archivos oficiales Binance

Bucket SOLUSDT15m abierto1619144100000 (2021-04-23T03:35:00Z), cerrado1619144999999:
- Derivado de monthly5m: open36.3239 high36.7999 low34.1 close34.8737 volume391787.77.
- Nativo15m monthly y daily: open36.3239 high36.8154 low34.1 close34.8737 volume403824.89.
- La PR hizo bien en detenerse: no es una convención de velas vacías.

La primera vela5m,1619144100000, difiere:
|Fuente5m|Open|High|Low|Close|Volume|Trades|
|---|---:|---:|---:|---:|---:|---:|
|Monthly abril2021|36.3239|36.7999|36.2002|36.6411|55374.15|1990|
|Daily23abril2021|36.3239|36.8154|36.2002|36.4699|67411.27|2493|

Las dos velas5m posteriores coinciden entre daily y monthly. El cierre del5m mensual primero (36.6411) no coincide con la apertura del siguiente(36.4699); el diario sí coincide. Este detalle refuerza el diagnóstico, pero continuidad de precios sola no sirve para inventar/reparar datos.

Sumar volúmenes daily5m:67411.27+194637.74+141775.88=403824.89; máximo36.8154. Su agregación OHLCV reproduce exactamente ambas versiones nativas15m. Monthly5m reproduce exactamente el derivado bloqueado. Diferencia de volumen12037.12. No asumir que daily sea universalmente superior: comprobación limitada a este bucket y cuatro archivos.

Se descargaron los cuatro ZIP y sus CHECKSUM oficiales; todos los SHA256 verificados:
- SOLUSDT-5m-2021-04.zip:7cf294f0cbff3c70dc8bfba2643f67690e5603495721f5f9239120dbbb522b9c
- SOLUSDT-15m-2021-04.zip:2d0d0484d2c9ab0a8c18e74ee689d82268624953735c155aa120dc98a0eba113
- SOLUSDT-5m-2021-04-23.zip:1fa5fc99ab292dec2e8faf068d42b7c00ecb10c29c32dcf51305b3a458d1ce2d
- SOLUSDT-15m-2021-04-23.zip:8f185d7ee0d6cffe83e10d3ad5acb9ebca0eaba1ed26ded66062a74d14745e8a

URLs reproducibles:
https://data.binance.vision/data/spot/monthly/klines/SOLUSDT/5m/SOLUSDT-5m-2021-04.zip
https://data.binance.vision/data/spot/monthly/klines/SOLUSDT/15m/SOLUSDT-15m-2021-04.zip
https://data.binance.vision/data/spot/daily/klines/SOLUSDT/5m/SOLUSDT-5m-2021-04-23.zip
https://data.binance.vision/data/spot/daily/klines/SOLUSDT/15m/SOLUSDT-15m-2021-04-23.zip
CHECKSUM añade .CHECKSUM a cada URL.

## Decisión y trabajo pendiente

No relajar tolerancias ni ignorar precios conflictivos. No sustituir únicamente el15m manteniendo5m discordante. Preparar una normalización auditable de5m basada en contraste diario/nativo: conservar hashes/versiones de ambos, verificar cobertura completa, rechazar discrepancias sin corroboración y reconstruir TODOS los intervalos dependientes y outcomes desde el nuevo input. Comprobar el alcance sobre el resto del día/activos antes de repetir matriz. No presentar un cambio de input como mejora predictiva.

Sin reparación implementada en esta revisión. Sin resultados nuevos de la comparación entre motores ni mejora demostrada. Cohorte15 no activa enmain. El rechazo previo de fusión por revisión automática continúa vigente: no intentar otro método/ref para eludirlo; resolver validación/autorización antes de integración. Sigue siendo posible investigar y conservar herramientas enrama, y recoger ADA enmain, por lo que el seguimiento no está totalmente bloqueado y se mantiene.

Noticias históricas ausentes/excluidas neutralmente; no se calculó rentabilidad. Mantener referencia67HIT/58FAIL/1NEUTRAL,53.6%.
