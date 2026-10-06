# Reparación de calibración y validación futura registrada

## Diagnóstico comprobado

La primera ejecución de 150 combinaciones terminó, pero todos los modelos
alternativos tenían cero casos de calibración. No es un fallo predictivo ni
una abstención al umbral del 65%: los modelos no llegaron a entrenarse.

En el BTC diario derivado hay un día incompleto el 24 de marzo de 2023
(apertura UTC 1679616000000). El filtro exige hasta 300 velas diarias seguidas
para el contexto Kotlin y 200 para las variables del alternativo. El contexto
diario compartido deja sin ventanas elegibles el periodo de calibración.
El archivo real descargado confirma cero ventanas consecutivas de 300 días
durante abril–septiembre de 2023. Hay velas en ese periodo; faltan ventanas
elegibles, no todo el histórico de mercado.

## Reparación

Se mantienen las velas 5m y sus huecos. Los buckets derivados incompletos de
15m, 30m, 1h, 4h y 1d se recuperan exclusivamente de archivos nativos oficiales
de Binance con CHECKSUM. Los buckets completos coincidentes se contrastan;
un conflicto de OHLCV detiene el trabajo. Nunca se interpolan precios.

Cada input conserva el hash anterior y la evidencia de la reparación. Los
plazos con 5m ausentes continúan PENDING. Se repite la reconstrucción de las
150 combinaciones porque recuperar contexto puede cambiar también las
decisiones y la memoria histórica del original. Las dos ejecuciones deben
identificarse por separado; el cambio de muestra no es una mejora del motor.

Los activos que realmente carezcan de entrenamiento/calibración suficiente
antes de octubre de 2023 siguen NO EVALUABLE en el examen fijo. No se les
prestan etiquetas posteriores para completar artificialmente la comparación.

## Validación nueva

El protocolo JSON fija 15 combinaciones elegidas tras inspeccionar el histórico.
Inicio: 2026-10-07 00:00 UTC; primera revisión temporal a los 180 días, sin
garantizar entonces suficientes señales o precisión estadística.

Para esta cohorte se preparan modelos separados: entrenamiento anterior a
2026-04-01 y calibración con etiquetas que terminan antes del corte histórico
2026-10-02 15:00 UTC. Estos modelos **no** se usan en el examen 2023–2026.
Sus hashes, protocolo y motor original quedan bloqueados al registrar la
cohorte. El umbral alternativo permanece en 0,65; el original en 81.

La captura horaria toma únicamente la última vela cerrada nueva de cada
combinación. No rellena retrospectivamente las velas omitidas. Las entradas
y ventanas se referencian al registro real: sólo se evalúa desde la primera
vela 5m que abre después de registrar el pronóstico. Se publican separados
aciertos, fallos, neutrales, pendientes, cobertura y modelos no disponibles.

Es una cohorte de investigación con retraso explícito y memoria reconstruida
(45 días intradía, 350 diarios, 120 días de 5m), no el estado persistido del
móvil. No es una simulación de ejecución o rentabilidad. Los resultados no
se comparan directamente con los porcentajes de la reconstrucción histórica.

No se declara ganador por máximo porcentaje. Para justificar una mejora
harán falta incertidumbre por bloques temporales, control de 15 comparaciones,
costes y suficientes señales; las ventanas de 14 días se solapan.

## Conservación y requisitos

Se consultó el documento maestro original de 59 puntos. Esta corrección
afecta a investigación, datos auditables y pruebas; conecta especialmente
con los puntos 7, 14, 18, 25–29, 43, 51, 53–55. No modifica producción
Android, alarmas, notificaciones, persistencia, temas o vigilancia híbrida.
La compilación/paridad en GitHub se requiere antes del nuevo examen.

Estado al publicar: implementado y probado localmente en Python; pendiente
del contraste nativo real, compilación Android y ejecución corregida en CI.
La validación prospectiva requiere integrar el flujo en main para que se
ejecute su horario, reunir pronósticos futuros y esperar sus horizontes.
