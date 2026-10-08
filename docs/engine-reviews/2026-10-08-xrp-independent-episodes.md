# XRP30m: seguimiento separado y control de solapamiento

Estado: herramientas comprobadas localmente, cohorte futura todavía no activa. Sin modificaciones Android ni integración en main.

La muestra histórica alternativa tiene 44 aciertos y 9 fallos (83,02%). Al agrupar transitivamente las ventanas de 24 horas solapadas, quedan cuatro episodios:

| Episodio | Señales | Aciertos | Fallos | Resultado de la primera señal |
|---|---:|---:|---:|---|
| 1 | 15 | 14 | 1 | Fallo |
| 2 | 8 | 8 | 0 | Acierto |
| 3 | 15 | 15 | 0 | Acierto |
| 4 | 15 | 7 | 8 | Acierto |

La primera señal de cada episodio da 3/4 (75%). Este es un diagnóstico exploratorio posterior al examen, no evidencia independiente ni rentabilidad. Las ventanas no solapadas tampoco garantizan independencia estadística.

Se registra un protocolo separado para XRP30m desde el 9 de octubre de 2026 UTC, con umbrales y preparación anteriores congelados. Si se activa después, comienza en el registro real; no se rellenan fechas perdidas. Se incluyen compras y ventas sin elegir la dirección por su resultado. El resumen principal selecciona la primera señal del episodio antes de examinar resultados y conserva también todas las señales. El protocolo original de 15 combinaciones se mantiene intacto.

`evaluation/xrp_episode_validation.py prepare` permite preparar el modelo prospectivo propio usando el histórico reparado y las decisiones Kotlin existentes; `capture` exige que ese modelo sea evaluable y que su hash y protocolo coincidan antes de registrar. Una ausencia de modelo bloquea la captura, no se presenta como cero señales evaluadas. `summarize` agrega un registro propio por motor.

La nueva tarea de GitHub prepara exclusivamente XRP y reutiliza las decisiones de la ejecución 37623666317 y el histórico existente: no repite la matriz de 150. Conserva el modelo propio y sus comprobaciones como artefacto. Las 55 pruebas Python locales pasan. La compilación Android de este nuevo commit sigue pendiente de GitHub.

Pendiente: confirmar la preparación del modelo XRP en GitHub, comprobar la captura real y mantener el registro en la rama de investigación. No hay recogida automática activa de esta cohorte. No se reutiliza el 83,02% como probabilidad de nuevas señales. No se fusiona la PR #9.
