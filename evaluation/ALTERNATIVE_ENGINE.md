# Motor alternativo — prototipo independiente

Autorizado por José Domingo el 2 de octubre de 2026. Se mantienen las funciones Android y los 59 requisitos. Implementación inicial fuera de la app: aprende directamente de velas cerradas, sin score, confianza ni decisiones del motor Kotlin como entradas. Usa árboles potenciados para BUY/SELL y calibración logística en un periodo posterior separado. División cronológica fija 60/20/20 y exclusión de horizontes de 24h que cruzan las fronteras.

Variables iniciales: retornos 1/4/12/24h, volatilidad, rango, volumen relativo y distancia de medias. No es aún la versión especializada por régimen: esa parte, el contexto MTF/BTC y el registro prospectivo de un modelo congelado quedan pendientes. El umbral 65% es experimental; no representa fiabilidad demostrada.

El informe guarda decisiones incluidas las descartadas, cobertura, muestra, precisión y Brier. Para las ventanas generales utiliza un objetivo propio de volatilidad; no comparar directamente su porcentaje con el 53,6% Kotlin. La sección `matched_same_targets` compara exclusivamente fechas, precios, objetivos y horizontes originales idénticos del tramo final, evaluados con las mismas velas 5m. Los casos correlacionados y la historia ya inspeccionada impiden declarar superioridad futura.

Tres pruebas: variables sin acceso al futuro, neutralidad cuando ambos niveles se tocan en una vela y dirección/primer toque. Actions reproduce el experimento. No integra alarmas, cambia la app ni promueve automáticamente el modelo. No mide beneficio ejecutable, comisiones ni deslizamiento.

Siguiente trabajo: revisar artefactos y comparación emparejada, ampliar historia, registrar protocolo antes de nuevos periodos, añadir contexto/régimen y congelar modelos con datos/versiones reproducibles para evaluación prospectiva. Sólo integrar en Android tras ventaja independiente demostrada y comprobaciones completas.
