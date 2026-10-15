# Seguimiento prospectivo — 15 de octubre de 2026, 08:40 Madrid

La primera señal alternativa TRX4h ya es HIT. Es un avance de validación, no una mejora predictiva independiente demostrada. Cuatro aciertos alternativos corresponden a la misma vela de resultado.

## Registro comprobado

| Cohorte | Decisiones | Señales | HIT | FAIL | NEUTRAL | Pendientes |
|---|---:|---:|---:|---:|---:|---:|
| XRP30m original | 26 | 4 | 2 | 1 | 0 | 1 |
| XRP30m alternativo | 26 | 0 | 0 | 0 | 0 | 0 |
| Seleccionadas15 original | 243 | 42 | 7 | 9 | 2 | 24 |
| Seleccionadas15 alternativo | 243 | 8 | 4 | 0 | 0 | 4 |
| ADA1h main | 54 | 7 | 2 | 4 | 0 | 1 |

Cortes de resúmenes: seleccionadas15 2026-10-15T06:37:18.663Z; XRP 2026-10-15T04:52:09.077Z. ADA última decisión registrada 2026-10-15T03:35:32.242Z. Fuentes recuperadas de GitHub, no de scratch. Las cohortes tienen entradas y muestras distintas.

Desde el informe diario del 14: seleccionadas15 +60 decisiones; original +11 señales, +3 HIT y +7 FAIL; alternativo sin nuevas señales, +3 HIT y cuatro pendientes restantes. XRP +5 decisiones y una nueva señal original pendiente. ADA +4 decisiones y una nueva señal pendiente; mantiene cuatro fallos.

## TRX: señal inicial y dependencia de los aciertos

| Señal alternativa | Registro UTC | Resultado | Vela de primer toque UTC |
|---|---|---|---|
| TRX-4h-1791719999999 | 11/10 15:10:00.065 | HIT | 12/10 03:39:59.999 |
| TRX-4h-1791734399999 | 11/10 19:47:47.185 | HIT | 12/10 03:39:59.999 |
| TRX-1h-1791759599999 | 12/10 00:00:30.563 | HIT | 12/10 03:39:59.999 |
| TRX-4h-1791748799999 | 12/10 00:00:30.566 | HIT | 12/10 03:39:59.999 |

Las ventanas completas vencieron después del toque; el evaluador conservó el criterio de esperar a disponer del horizonte requerido. El toque no es la fecha de publicación del resultado.

La primera TRX4h, cuya ventana terminó el 14/10 a las 17:14:59.999 Madrid, ya está resuelta HIT. Las ocho compras alternativas TRX siguen formando un único grupo transitivo multitemporal, seleccionado sin consultar resultados. Su primera señal es ahora HIT. No son cuatro pruebas independientes ni una tasa fiable del 100%.

Las siete señales alternativas 4h: 3 HIT y 4 pendientes; 1h: 1 HIT. Las cuatro ventanas 4h restantes vencen entre el 16/10 01:19:59.999 y el 16/10 21:39:59.999 Madrid. Resolver sólo con datos oficiales completos.

Tres decisiones fueron aceptadas por ambos motores: una resuelta (TRX4h, original SELL FAIL frente a alternativo BUY HIT) y dos pendientes. Es una comparación pareada favorable al alternativo en un caso, dentro del mismo episodio; insuficiente para inferencia de superioridad.

## Episodios y calibración

Agrupación diagnóstica por activo/motor de ventanas aceptadas, incluso entre plazos: original 6 grupos, primeras señales 2 HIT (RUNE), 1 NEUTRAL (TRX), 3 pendientes (ATOM, ADA, ETH); alternativo 1 grupo TRX con primera señal HIT. No reemplaza las métricas por combinación ni demuestra independencia estadística.

XRP: 3 episodios originales, primera señal 2 HIT y 1 pendiente; alternativo sin señales aceptadas. Sus 20 puntuaciones resueltas tienen Brier 0,246500; media estimada 0,51647 frente a 0,60 observado. No confundir puntuaciones bajo umbral con señales operables.

TRX4h alternativo: Brier 0,126334 en sólo 9 puntuaciones resueltas, dependientes y con señales solapadas. TRX15m/30m/1h: 0,269400 / 0,267271 / 0,271207 en 11 puntuaciones cada uno. El dato favorable 4h no se extrapola a otros plazos ni demuestra calibración robusta.

## Integridad y ejecuciones

Se contrastaron los 59 puntos originales de Library antes de cualquier posible cambio de código. Se añadió únicamente una herramienta Python aislada de simulación y su verificación; no se modificó código Android ni la captura existente.
Auditoría del journal seleccionado: 243 decisiones únicas; cero discrepancias de protocolo, motor o hash de modelo respecto a freeze-lock; 243 recibos presentes y confirmados antes de first_open. Se reconstruyeron los recuentos de señales y grupos sin escoger retrospectivamente resultados favorables.

- Última recogida seleccionadas15 completada: [38691837238, intento 2](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/38691837238), success; recuperó 234 y añadió 9 decisiones, con 5 nuevas señales originales pendientes. Logs: 70 tests Python y testDebugUnitTest, BUILD SUCCESSFUL.
- XRP [38683566265](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/38683566265), success; recuperó 25 y añadió 1 decisión.
- ADA [38676940231](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/38676940231), success.
- Verificación de la herramienta nueva [38693622444](https://github.com/Domingales/Cripto-Analisis-Multi---MOTOR-PREDICTIVO/actions/runs/38693622444), success: 85 pruebas Python (70 anteriores y 15 nuevas). Código publicado en c864a9b07979c60ff7b9bfd6844dd969ac732fe1.
- Artefacto seleccionado del reintento correcto 11952996706, digest sha256:3b32b608b3f79fd2cdb92d3792fd5e6b41e43c0e917948c02deaf0d1f44fd277; XRP 11949386210, digest sha256:11c93918bf65549c3926d4d986b30d286be392f47c42d6b4ea156f195967ca6b; ambos vigentes.
- Journal seleccionado auditado: blob bc31bf6be5338c2a95ed625b9960133d58060713; resumen blob 2ac6b2d6c0427f33f8f506e10e61c0df64ff44c5. XRP resumen d657cbe89ec3763a7664b92e03180544a9c14141.
- Protocolo seleccionado e0a5fe96c1cab432a39ebbcca95ecbdc167d287a714043b4e04f1a4e95fd47dc; motor f8fda6db64703e3b7fbfd1edc25aa8f20a544ed2a4b6a965970c9ec6e691d5e7. Modelos y umbrales congelados.
- Recogidas configuradas horarias, pero los últimos intervalos seleccionados siguen siendo de varias horas (aproximadamente 2,2–6,0 h). No afirmar vigilancia horaria efectiva ni reconstruir decisiones omitidas.

La clonación local volvió a fallar por conectividad del proxy; el acceso GitHub permitió completar la auditoría y publicación. No es un bloqueo total. PR9 sigue abierta y no fusionable; no se necesita fusionarla para esta investigación. No se forzaron refs, repitió matriz150 ni modificó producción.

## Trabajo añadido y siguiente paso

Se publicó [un protocolo adicional de ejecución y costes](2026-10-15-execution-costs-preregistration.md), commit e185044ae5f2d2887ed4518d6a67cf2a1a676c9b, reservado a decisiones desde 2026-10-16T00:00:00Z. Define entrada posterior a registro durable, costes/sensibilidades fijos, gaps, ambigüedad intravela y bloqueo por activo. El núcleo independiente evaluation/execution_costs.py ya está implementado y comprobado con 15 pruebas nuevas; las 85 del conjunto pasan en Actions. Verifica hashes congelados y recibos, reserva temporal, entrada posterior, costes por lado, gaps, intervalos de doble toque y bloqueo por activo; conserva las etiquetas direccionales y separa MFE/MAE de beneficio. No hay todavía resultados de mercado de esta simulación ni rentabilidad demostrada.

Siguiente trabajo: aplicar la herramienta a decisiones registradas desde el inicio futuro sólo cuando maduren sus ventanas, conservar entradas/velas/manifiestos y ampliar los informes por régimen y bloque sin escoger ganadores; continuar resolviendo ventanas vencidas, incluyendo el resto TRX4h, sin cambiar la selección ni optimizar con resultados vistos. No trasladar a producción el alternativo por un único episodio favorable.

## Colisión de publicación resuelta

El intento 1 de 38691837238 falló a las 08:28 Madrid: la publicación del protocolo e185044 avanzó la rama mientras la recogida partía de 3451b0f. El push fast-forward rechazó sobrescribir el trabajo nuevo y el recolector abortó. La publicación de esta revisión causó la colisión. No fue un bloqueo de revisión automática ni un fallo del motor.

Se actualizó el informe antes de reintentar únicamente el job fallido. Durante el reintento no se publicaron cambios en la rama. El intento 2 recuperó 234 decisiones y registró 9 decisiones actuales a las 08:37:18 Madrid, con primera vela de resultado a las 08:40 y recibos anteriores a ella. No se importaron eventos desde el artefacto del intento fallido ni se retrofechó su registro. Después de terminar correctamente se publicó la herramienta de costes. Para futuras revisiones, esperar a que no haya escritores activos antes de publicar en esta rama; mantener la protección que aborta ante trabajo remoto nuevo.
