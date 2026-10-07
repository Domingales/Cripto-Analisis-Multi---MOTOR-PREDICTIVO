# Excepción cronológica del adaptador de investigación

La revisión adicional del requisito «sin información futura» identifica una
dependencia en `BacktestEngine`: después de aceptar una señal, si la ventana
futura no contiene todas sus velas de 5m, continúa sin actualizar
`lastAccepted`. Eso puede cambiar las aceptaciones siguientes durante el
cooldown, antes de llegar a la vela ausente. Cambiar precios futuros no detecta
este caso; hay que cambiar también su disponibilidad.

La aplicación de producción no se modifica. El generador de la copia de
investigación inserta una sola asignación de `lastAccepted` inmediatamente
después de `onAccepted`, antes de examinar resultados futuros. Las asignaciones
originales posteriores son redundantes y permanecen para minimizar el diff.
No se cambian umbrales, indicadores, noticias, horizontes ni etiquetas.

La prueba Kotlin mantiene la igualdad completa de análisis, aceptaciones y
resultados con el motor original en datos completos. Añade una vela futura
ausente y exige que el adaptador conserve todos los análisis y aceptaciones
anteriores. La misma prueba exige observar la diferencia en el original sin
modificar: es una excepción explícita a la equivalencia en entradas con huecos,
no una igualdad que pueda afirmarse universalmente.

La ejecución 37600024841 verifica la reparación de datos pero precede a esta
corrección de reloj. Sus informes no constituyen el experimento final. El nuevo
commit reinicia la puerta de verificación y las 150 combinaciones; la cancelación
de la ejecución anterior por el paso de repeticiones obsoletas es deliberada,
no una avería de infraestructura. No se deben mezclar las dos versiones.

Estado al escribir: 56 pruebas Python pasan; la nueva prueba Kotlin todavía debe
ser comprobada en CI. Los resultados finales deben denominar al baseline
«original con adaptador cronológico seguro» y explicar esta excepción.
