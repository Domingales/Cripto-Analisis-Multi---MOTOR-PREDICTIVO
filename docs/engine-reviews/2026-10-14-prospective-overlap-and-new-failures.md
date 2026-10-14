# Seguimiento prospectivo — 14 de octubre de 2026, 08:10 Madrid

No hay mejora predictiva independiente demostrada. Se recuperaron los registros actuales de GitHub; no se repitió la matriz de 150 ni se reentrenó ningún modelo.

## Resultados verificados

| Cohorte | Decisiones | Señales aceptadas | HIT | FAIL | NEUTRAL | Pendientes |
|---|---:|---:|---:|---:|---:|---:|
| XRP30m original | 21 | 3 | 2 | 1 | 0 | 0 |
| XRP30m alternativo | 21 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 183 | 31 | 4 | 2 | 2 | 23 |
| Seleccionadas15 alternativo | 183 | 8 | 1 | 0 | 0 | 7 |
| ADA1h main | 50 | 6 | 2 | 4 | 0 | 0 |

Cortes: XRP 2026-10-14T05:03:55Z; seleccionadas15 2026-10-14T02:13:49Z; ADA última escritura 2026-10-14T03:54:07Z. Son muestras distintas: no comparar sus porcentajes como un experimento emparejado.

Respecto al informe del 13 por la mañana: XRP +4 decisiones; seleccionadas15 +42 decisiones, original +16 señales y +3 HIT (RUNE), alternativo +2 señales y ningún nuevo resultado aceptado; ADA +4 decisiones y dos FAIL más. Respecto al aviso de ayer por la tarde, el original seleccionado suma sólo un HIT adicional.

## Control de solapamiento sin usar resultados

Se agruparon transitivamente las ventanas aceptadas por activo y motor, incluyendo distintos plazos. Esto es un diagnóstico adicional de dependencia; no modifica los protocolos ni reemplaza sus métricas por combinación.

Original seleccionado: 5 grupos de ventanas: ATOM pendiente; TRX neutral; RUNE dos grupos cuya primera señal fue HIT; ADA pendiente. Alternativo: las ocho compras TRX pertenecen a un único grupo; su primera señal, TRX4h del 11 de octubre, sigue pendiente. El HIT posterior de TRX1h no puede sustituirla. Por tanto, no hay aún un primer resultado alternativo resuelto a nivel de este grupo multitemporal. Ventanas no solapadas tampoco prueban independencia estadística.

La primera ventana TRX4h alternativa termina el 14 de octubre a las 17:14:59 Madrid. Resolver sólo cuando existan todos los datos oficiales necesarios; no convertir ausencia de datos en fallo ni escoger retrospectivamente otra señal.

XRP conserva dos episodios, ambos con primera señal HIT, aunque las tres señales individuales dan 2 HIT / 1 FAIL. No trasladar el histórico exploratorio 83,02% al modelo nuevo.

## Calibración y límites

XRP alternativo: 17 puntuaciones resueltas, Brier 0,245140; probabilidad media 0,51733 y frecuencia HIT 0,64706. Incluye puntuaciones bajo umbral; no son 17 señales operables. Muestra breve y dependiente: no demuestra ventaja.

El único HIT aceptado alternativo continúa siendo TRX1h. Sus siete TRX4h pendientes y el solapamiento impiden anunciar mejora estable. ADA incorpora dos fallos nuevos; no ocultarlos. No hay simulación nueva de costes ni rentabilidad demostrada.

## Continuidad y verificación

- XRP run 38514444333 success: recuperó 20 decisiones y añadió 1, total 21; modelo 009e602aef903ce3ff3b6ce8000f07ad5c38fbd6c0d4839d14167f8acfaa21d3; protocolo 998eab68bd77ad35dbf57676c1ec39c38d0a991436676d01de7fdfe6b3102ebd.
- Seleccionadas15 run 38499322898 success: recuperó 168 decisiones y añadió 15, total 183; protocolo e0a5fe96c1cab432a39ebbcca95ecbdc167d287a714043b4e04f1a4e95fd47dc.
- Logs seleccionadas15: 70 pruebas Python y testDebugUnitTest, BUILD SUCCESSFUL. No se modificó código Android.
- Artefactos vigentes: XRP 11871186951, digest b105def96e12e3787bfb0271d47e317b0e78d1897bc4fa3c38ae1837b64a875d; seleccionadas15 11865447327, digest 66fbdc0444073b10d080e0224bc6d71f93a46128571c977fd8fb2713eecbc404.
- Registros durables en GitHub continúan creciendo; no se depende de scratch ni exclusivamente de artefactos.
- Recogidas configuradas horarias, pero los tres últimos registros aparecen separados aproximadamente cuatro horas. No afirmar vigilancia horaria efectiva ni rellenar retrospectivamente decisiones omitidas.

PR9 sigue abierta y no fusionable. No se fusionó, no se cambiaron umbrales, protocolos, modelos ni producción. La clonación local falló por conectividad del proxy; la recuperación y publicación se realizan mediante el conector GitHub, sin impedir este control.

## Próximo paso

Comprobar la primera ventana TRX4h y las demás ventanas vencidas en la siguiente recogida, mantener el análisis por episodios y comparar sólo casos emparejados. Registrar toda hipótesis futura de ejecución/costes antes de evaluarla; no optimizar con estos resultados ya vistos. No introducir filtros en producción sin ventaja independiente.
