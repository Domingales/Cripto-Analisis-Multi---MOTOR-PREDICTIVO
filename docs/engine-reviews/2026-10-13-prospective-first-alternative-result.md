# Seguimiento prospectivo — 13 de octubre de 2026

Revisión cerrada a las 08:10 Europe/Madrid sobre registros durables publicados. No se ha reentrenado ningún modelo, modificado umbrales ni tocado el motor Android de producción. La matriz histórica de 150 combinaciones no se ha repetido.

## Resumen acumulado

| Cohorte / motor | Decisiones | Señales | HIT | FAIL | NEUTRAL | Pendientes |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| XRP30m original | 17 | 3 | 2 | 1 | 0 | 0 |
| XRP30m alternativo | 17 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 141 | 15 | 1 | 2 | 2 | 10 |
| Seleccionadas15 alternativo | 141 | 6 | 1 | 0 | 0 | 5 |
| ADA1h original, main | 46 | 6 | 2 | 2 | 0 | 2 |

Los neutrales no se cuentan como acierto ni fallo. Los intervalos de Wilson al 95 % sobre resultados direccionales resueltos son muy amplios: XRP original 66,7 % (20,8–93,9 %), seleccionadas15 original 33,3 % (6,1–79,2 %), ADA 50 % (15,0–85,0 %) y seleccionadas15 alternativo 1/1 (20,7–100 %). Ninguna cifra demuestra ventaja.

## Resultados nuevos

- Las señales originales vencidas de la cohorte15 quedaron: TRX30m SELL NEUTRAL, TRX1h SELL NEUTRAL, RUNE1h SELL HIT, y dos TRX1h SELL posteriores FAIL.
- La primera señal aceptada y resuelta del alternativo, TRX1h BUY, fue HIT. Es exactamente una señal resuelta y no permite afirmar fiabilidad.
- Continúan pendientes cinco señales alternativas BUY en TRX, principalmente TRX4h, con ventanas solapadas. No son cinco pruebas independientes.
- El original suma diez pendientes: ATOM1d BUY y señales recientes de RUNE, TRX y ADA en 30m/1h/4h.
- ADA1h resolvió como FAIL el SELL y el BUY que estaban pendientes. Su historial prospectivo queda 2 HIT y 2 FAIL; añade dos SELL todavía abiertos.
- XRP no emitió señales nuevas: mantiene 2 HIT y 1 FAIL en tres señales. Por la regla congelada de episodios, son dos primeros avisos de episodio y ambos HIT, pero dos episodios son insuficientes y no prueban independencia.
- El alternativo XRP sigue en cero señales aceptadas. Trece puntuaciones direccionales vencidas dan Brier 0,245911, probabilidad media 0,5175 y frecuencia observada 0,6154; muestra pequeña y sin ventaja demostrada.

## Continuidad y verificaciones

- XRP run 38342560577: success; recuperó 16 decisiones y añadió una hasta 17. Modelo y protocolo conservan sus hashes congelados. Artefacto 11787531564, digest sha256:fbd4f29fc2812fccba963f26b79457077bc1d51ac7d125aaa9864e5266bb2788.
- Seleccionadas15 run 38343282642: success; recuperó 126 decisiones y añadió 15 hasta 141. Protocolo congelado intacto. Artefacto 11788415760, digest sha256:a6b7c18037a9242a3921f8f7bab9793a2cc58fbe234647de70e58b87ba10f3e9.
- ADA run 38334123859: success. Artefacto 11784296979, digest sha256:50bf8d29915e2f8a19606c1f989626a5ffb61415fc894e7b54c688b42d5cac1b.
- Las dos recogidas de investigación ejecutaron 70 pruebas Python y testDebugUnitTest con BUILD SUCCESSFUL.
- PR9 continúa abierta y no fusionable. La recogida separada sigue activa; no se ha fusionado la PR ni cambiado producción.
- Las ejecuciones programadas siguen presentando intervalos de varias horas, por lo que “horario” describe la configuración, no una garantía de ejecución continua. No se rellenan retroactivamente velas omitidas.

## Interpretación y próximo control

El resultado más interesante es el desacuerdo TRX: dos SELL originales fallaron y el primer BUY alternativo acertó. No debe convertirse todavía en un filtro ni en una selección retrospectiva: las señales comparten activo, régimen y ventanas, y el alternativo sólo tiene un caso resuelto. Se mantendrán congelados ambos motores hasta resolver las ventanas TRX4h y reunir episodios separados.

El próximo control resolverá las señales intradía vencidas con velas oficiales, agrupará solapamientos antes de contar evidencia y evaluará las señales TRX4h enfrentadas sin ajustar el modelo. La simulación de costes debe fijarse antes de usar estos resultados para cualquier decisión: entrada posterior a escritura durable, comisión, spread, deslizamiento, una posición por episodio y tratamiento conservador de ambigüedad intravela.

Conclusión: infraestructura y seguimiento correctos; evidencia predictiva todavía insuficiente. No procede promover cambios a producción.

Fuentes: evaluation/prospective-xrp/{summary.json,episodes.json,collection-runs.json}; evaluation/selected-prospective/{summary.json,events-2026-10.jsonl,collection-runs.json}; evaluation/prospective/events.jsonl en main; ejecuciones y artefactos citados.
