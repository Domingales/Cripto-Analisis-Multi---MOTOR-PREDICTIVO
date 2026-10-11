# Seguimiento prospectivo — 11 de octubre de 2026

Revisión cerrada a las 08:55 Europe/Madrid sobre datos durables ya publicados. No se ha reentrenado ningún modelo, no se han modificado umbrales y no se ha tocado el motor Android de producción.

## XRP 30m congelado

- 10 ejecuciones durables consecutivas; la última recuperó 8 decisiones y añadió 1, para 9 decisiones únicas.
- Los hashes permanecen fijos: modelo `009e602aef903ce3ff3b6ce8000f07ad5c38fbd6c0d4839d14167f8acfaa21d3` y protocolo `998eab68bd77ad35dbf57676c1ec39c38d0a991436676d01de7fdfe6b3102ebd`.
- Motor original: 3 señales SELL; 1 HIT, 1 FAIL y 1 pendiente. Precisión provisional por decisión resuelta: 1/2 (50 %).
- Las dos primeras señales se solapan transitivamente y forman un solo episodio. La primera señal del episodio fue HIT; por episodios cerrados hay 1/1, pero esta unidad no es independencia estadística ni evidencia suficiente.
- El tercer SELL abre un segundo episodio y vence el 12 de octubre a las 03:09:59 Europe/Madrid.
- Modelo alternativo: 0 señales aceptadas. En las cuatro puntuaciones direccionales ya resolubles, Brier 0,258679; probabilidad media 0,5162 frente a frecuencia observada 0,25. La muestra es demasiado pequeña y, por ahora, muestra sobreconfianza, no ventaja.
- Excursiones de las señales originales resueltas: HIT con MFE +0,699 % y MAE -1,808 %; FAIL con MFE +0,094 % y MAE -1,853 %. Son movimientos brutos de mercado, no rentabilidad neta ni simulación de ejecución.

## Cohorte congelada de 15 combinaciones

- 5 ejecuciones durables consecutivas; la última recuperó 42 decisiones y añadió 15, para 57 decisiones únicas.
- Protocolo fijo `e0a5fe96c1cab432a39ebbcca95ecbdc167d287a714043b4e04f1a4e95fd47dc`.
- Motor original: 4 señales pendientes: ATOM-1d BUY (ya existente), más TRX-30m SELL, TRX-1h SELL y RUNE-1h SELL nuevas. Las tres señales intradía vencen el 12 de octubre a las 03:24:59 Europe/Madrid; ATOM-1d mantiene su horizonte largo.
- Motor alternativo: 0 señales aceptadas y todavía 0 puntuaciones resolubles; no procede calcular precisión, Brier ni calibración.
- Las 15 combinaciones siguen separadas del flujo XRP y conservan una sola decisión por combinación y vela.

## Registro ADA 1h de `main`

- 40 decisiones registradas, 3 señales: 2 HIT cerrados y 1 SELL pendiente.
- La nueva señal SELL vence el 12 de octubre a las 06:34:59 Europe/Madrid.
- El 2/2 previo continúa siendo una muestra insuficiente para afirmar fiabilidad o mejora.

## Conclusión y siguiente control

La persistencia, recuperación, deduplicación y continuidad están funcionando. No hay fallos ni neutros nuevos en la cohorte seleccionada; XRP aporta el primer contraste real con un HIT y un FAIL solapados. No hay mejora predictiva demostrada y no se promueve nada a producción.

El siguiente control debe resolver las ventanas XRP/TRX/RUNE/ADA que vencen el 12 de octubre, conservar los huecos como pendientes si faltan velas oficiales y actualizar Brier/calibración únicamente con puntuaciones ya vencidas. Después podrá iniciarse la simulación de ejecución y costes, sin usar estos resultados para reajustar los modelos congelados.
