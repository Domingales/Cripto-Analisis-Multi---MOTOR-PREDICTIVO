# Primera auditoría del motor predictivo

El flujo `Auditoría de precisión ADA 1h` se ejecuta automáticamente cuando se actualizan sus archivos o el motor en la rama `main`. También se puede iniciar desde **Actions → Run workflow**. Descarga velas públicas de Binance Spot desde su API oficial de datos de mercado `data-api.binance.vision` (sin intermediarios) ADAUSDT y BTCUSDT hasta el 29 de septiembre de 2026, 00:00 UTC. El corte fijo permite repetir exactamente la comparación con una futura versión del motor. Se conservan las velas CSV, un manifiesto, cada señal evaluada y un resumen en el artefacto de GitHub Actions. La fecha puede ajustarse mediante `EVALUATION_END_UTC` al ejecutar el descargador localmente, pero una comparación entre versiones debe usar el mismo corte y las mismas velas.

El motor de producción se ejecuta con ADA 1h, umbral 81, contexto ADA 4h/1d y BTC 4h. Las noticias históricas no disponibles se excluyen mediante contexto neutral. Cada señal se resuelve por primer toque de objetivo o stop con velas de 5 minutos **cerradas** durante las siguientes 24 horas. Un toque de ambos niveles dentro de la misma vela es neutral porque no se conoce el orden. Si falta alguna vela de 5 minutos, el caso queda pendiente y la auditoría falla antes de presentar una precisión incompleta.

El resumen separa BUY/SELL y presenta precisión sólo entre HIT y FAIL, neutros, cobertura de señales sobre ventanas evaluadas y el último tramo cronológico. No representa una validación futura independiente ni garantiza rentabilidad; no incluye comisiones, deslizamiento ni ejecución real. Antes de modificar reglas predictivas conviene fijar este artefacto como referencia y comparar exactamente los mismos casos. Una medición prospectiva posterior necesitará registrar señales nuevas antes de conocer sus resultados.

Ejecución local con Android SDK y JDK 17:

```bash
MARKET_DATA_DIR="$PWD/evaluation/data" python3 evaluation/fetch_binance.py
MARKET_DATA_DIR="$PWD/evaluation/data" MARKET_REPORT_DIR="$PWD/evaluation/output" \
  bash ./gradlew testDebugUnitTest --tests com.domingales.criptoanalisis.multi.evaluation.MarketBaselineTest
```
