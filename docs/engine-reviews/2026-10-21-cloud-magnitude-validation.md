# Pruebas reales sin instalar en el móvil

Los recolectores programados de XRP 30m y de 15 combinaciones ejecutan el
predictor Kotlin de la aplicación en GitHub con velas oficiales actuales. El
alternativo permanece congelado. No se realizan operaciones ni se mueve dinero.

Se añade MAGNITUDE_TIME_V1 a cada decisión nueva, antes de su confirmación remota:
objetivo medio del rango del original, 24h exacto, mínimo 75% a 48h, máximo 120h.
El mismo objetivo sirve de referencia para ambos motores; sus direcciones y
filtros de aceptación siguen siendo propios. No se afirma que el alternativo
haya aprendido a pronosticar ese recorrido. La nueva regla no calibra ni cambia
las probabilidades de ninguno de los motores.

Las decisiones antiguas no se reinterpretan como pronósticos prospectivos de
este criterio. No se rellenan retrospectivamente las velas no observadas. Las
recepciones remotas verifican hash y confirmación anterior a la primera vela
admitida. Los plazos empiezan al registrar, no al cierre antiguo del análisis.

La evaluación reutiliza directamente ForecastCriterion.kt mediante el test JVM
MagnitudeLiveTest; no hay una segunda implementación Python del evaluador.
Se conservan perfiles, código hash, fuente de precios hash, excursiones y toques.
Huecos quedan pendientes. Se guardan resultados terminales append-only y un
resumen separado: magnitude-summary.json y magnitude-outcomes.jsonl en cada
cohorte. Se distinguen tasas exactas y aproximadas y primeras señales de ventanas
no solapadas de cinco días por cripto, intervalo y motor. No hay ganador declarado.

Estas son pruebas de pronósticos en un entorno remoto; no comprueban batería,
notificaciones, persistencia SQLite ni funcionamiento físico Android. El precio
de referencia remoto es el último cierre 5m conocido, distinto del precio de la
vela principal que usa el móvil. Los resultados permanecen en cohortes separadas.

## Integridad del predictor congelado

La incorporación del evaluador independiente añadía un archivo al comodín de
dominio y cambiaba el hash agregado sin cambiar el predictor. El helper
frozen_engine_digest.py enumera los siete archivos originales y reproduce
exactamente el hash anterior f8fda6db… conservado en los candados. No se cambia
ningún candado, modelo, umbral ni protocolo antiguo. Una prueba verifica tanto
la igualdad como la detección de cambios en SignalEngine.kt.

La última recogida de 15 combinaciones anterior a esta corrección falló al
intentar publicar sobre una rama que avanzó mientras se ejecutaba. La escritura
se rechazó sin sobrescribir nada. Las decisiones que no llegaron a confirmarse
remotamente a tiempo no se rescatan como prospectivas.

## Continuidad y comprobación

Los lanzadores ya programados en main descargan las herramientas de investigación
y ejecutan las nuevas fases al final de sus scripts. Un workflow adicional en
investigación lanza una comprobación real inicial al publicarse. La programación
es horaria, pero GitHub puede retrasar u omitir ejecuciones; no se promete una
captura cada hora exacta. No se requiere que el usuario abra ni instale la app.

Los contratos necesitan tiempo para madurar: hasta 24h, 48h o 120h, más el retraso
de la siguiente ejecución. Tener captura y evaluador funcionando no demuestra
una mejora predictiva. Los artefactos de Actions contienen los contratos y la
evidencia de cada ejecución; el resumen persistido muestra el estado real.
