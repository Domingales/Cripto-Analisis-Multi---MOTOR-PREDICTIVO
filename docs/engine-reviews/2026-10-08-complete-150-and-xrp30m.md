# Revisión 2026-10-08: repetición completa y análisis de XRP 30m

Revisión: 2026-10-08 08:54 Europe/Madrid.

## Estado verificado

- PR #9 sigue abierta y no integrada en `main`.
- Head de investigación: `fde000fa2aa1aea1fbed4ac0303b165cf6250b26`.
- Ejecución 37623666317: `success`.
- La ejecución verificó 150 informes únicos, sin faltantes.
- Verificación previa, compilación Android, auditoría fija y pruebas de ausencia de futuro: correctas.
- Los cambios permanecen en investigación; no se modificó el motor Android de producción.
- Se leyó el documento maestro original de 59 puntos antes de esta revisión.

## Comparación completa

Periodo de examen: 2023-10-02 hasta el corte fijo 2026-10-02T15:00Z. Resolución de primer toque con velas posteriores de 5 minutos. Noticias históricas ausentes: excluidas neutralmente.

| Motor | Decisiones | Señales | HIT | FAIL | NEUTRAL | PENDING | Precisión resuelta | Cobertura |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Kotlin actual reconstruido | 5.690.303 | 557.524 | 280.167 | 274.779 | 1.836 | 742 | 50,485 % | 9,798 % |
| Alternativo fijo | 5.690.303 | 3.951 | 1.570 | 2.213 | 167 | 1 | 41,501 % | 0,069 % |

El alternativo quedó correctamente entrenado/calibrado en 134 combinaciones. No evaluables por falta real de preparación anterior a octubre de 2023:
- APT: 15m, 30m, 1h, 4h y 1d.
- ARB: 15m, 30m, 1h, 4h y 1d.
- SUI: 15m, 30m, 1h, 4h y 1d.
- OP: 1d.

No se desplaza el examen ni se usan etiquetas futuras para rellenarlas.

### Por intervalo

| Intervalo | Kotlin HIT/FAIL | Kotlin precisión | Alternativo HIT/FAIL | Alternativo precisión |
|---|---:|---:|---:|---:|
| 15m | 96.847 / 97.520 | 49,827 % | 64 / 120 | 34,783 % |
| 30m | 85.450 / 84.026 | 50,420 % | 308 / 420 | 42,308 % |
| 1h | 74.001 / 71.389 | 50,898 % | 911 / 1.461 | 38,406 % |
| 4h | 21.527 / 19.955 | 51,895 % | 23 / 8 | 74,194 % |
| 1d | 2.342 / 1.889 | 55,353 % | 264 / 204 | 56,410 % |

El 4h alternativo tiene sólo 31 casos resueltos y no debe compararse como si tuviera la evidencia de los intervalos con miles de casos. El diario alternativo es la única agrupación amplia por encima del 50 %, pero su ventaja bruta sobre Kotlin es pequeña y procede de selecciones escasas/desiguales.

## XRP 30m: por qué aparece 83,02 %

Resultado: 44 HIT y 9 FAIL, 53 señales, todas SELL. Cobertura: 53 de 52.638 decisiones (0,101 %). Probabilidades aceptadas: 0,6502–0,6717.

Comprobación temporal de los registros crudos:
- Primera señal: 2024-05-19T00:59:59.999Z.
- Última señal: 2024-06-30T05:59:59.999Z.
- Las 53 señales aparecen en sólo 6 días naturales.
- Al agrupar señales separadas por más de 24 horas quedan sólo 4 episodios de mercado: tamaños 15, 8, 15 y 15; resultados 14/1, 8/0, 15/0 y 7/8.
- Intervalo Wilson ingenuo al 95 % tratando 53 señales como independientes: 70,77–90,80 %. Ese intervalo es optimista porque las ventanas de 24 horas se solapan y los casos dentro de cada episodio están fuertemente correlacionados.
- No hubo señales XRP30m alternativas en 2023, 2025 ni 2026.
- La calidad probabilística sobre todas las decisiones es casi aleatoria: Brier 0,24979; el máximo selectivo no representa el comportamiento general del modelo.
- El resultado de 83,02 % fue descubierto al inspeccionar 150 combinaciones. Existe sesgo de selección múltiple.

Conclusión: hay una hipótesis interesante y concreta —XRP30m SELL en el patrón que el modelo puntuó por encima de 0,65—, pero no 53 pruebas independientes ni evidencia de que el patrón funcione fuera de mayo-junio de 2024. El último episodio pasó de 7 aciertos iniciales a 8 fallos consecutivos, señal clara de cambio de régimen.

## Decisión de ingeniería

No integrar el alternativo ni copiar este filtro a otras criptomonedas. No retocar el umbral con el periodo ya inspeccionado. La forma correcta de investigar si se puede trasladar es:
1. congelar las variables/modelo actuales;
2. registrar XRP30m SELL antes de conocer el desenlace;
3. exigir episodios nuevos separados, no contar como independientes todas las señales solapadas;
4. contrastar por bloque temporal y régimen;
5. medir cobertura, Brier, costes y BUY/SELL por separado;
6. estudiar después si el mismo patrón aparece en otras criptomonedas, manteniendo una reserva no usada.

El protocolo prospectivo existente de 15 combinaciones ya está congelado y tiene registros; modificarlo invalidaría sus hashes. XRP30m debe usar una cohorte separada o incorporarse únicamente a un protocolo nuevo, nunca reescribir el ya iniciado. La activación periódica sigue condicionada a integración autorizada en `main`.

## Registro prospectivo de main

A esta revisión:
- 30 decisiones ADA1h registradas.
- 2 señales aceptadas.
- 2 HIT, 0 FAIL, 0 NEUTRAL, 0 pendientes.
- Son 2 decisiones nuevas y la señal pendiente anterior ya se resolvió como HIT.
- Dos aciertos no demuestran una precisión estable.

Referencia fija conservada: 67 HIT, 58 FAIL, 1 NEUTRAL; 53,6 %. No es directamente comparable con la reconstrucción ampliada.

## Estado de mejora

No hay mejora predictiva demostrada ni cambio de producción. El alternativo global es claramente inferior. XRP30m queda como hipótesis de validación, no como regla aprobada. No se afirma rentabilidad.
