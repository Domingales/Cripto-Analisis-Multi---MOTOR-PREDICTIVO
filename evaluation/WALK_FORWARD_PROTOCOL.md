# Experimento cronológico de dos motores — autorizado 2026-10-02
Estado: protocolo registrado; descarga completa y simulación pendientes.

## Orden acordado
1. Comparación inicial con código y modelo entrenado congelados.
2. Ensayo separado de reentrenamiento mensual simulado.
3. Análisis de errores y mejoras al final; validación independiente antes de activar cambios.

## Fechas y alcance inicial
ADAUSDT 1h, contexto ADA 4h/1d y BTC 4h, resolución 5m.
Datos desde 2020-10-02T00:00:00Z; examen desde 2023-10-02T00:00:00Z.
Corte reproducible final: 2026-10-02T15:00:00Z (17:00 Europe/Madrid). Sólo velas cerradas.
Convención UTC declarada para reproducibilidad; no confundir con hora local.
Los resultados con horizonte posterior al corte quedan pendientes, nunca fallos.
Ampliar monedas/intervalos sólo después de validar esta primera ruta.

## Acceso a datos
Archivos oficiales Binance Spot diarios/mensuales con CHECKSUM; API oficial para completar días recientes.
Normalizar timestamps: archivos Spot desde enero de 2025 usan microsegundos.
Guardar manifest con origen, fechas, hashes, conteos y huecos. No inventar ni rellenar precios ausentes.
La prueba local de acceso al archivo ADAUSDT 1h 2020-10 agotó 15 segundos sin recuperar bytes: disponibilidad específica aún no verificada.
No afirmar que los seis años están descargados.

## Motor fijo y frontera de información
Congelar reglas/código de ambos motores y artefacto entrenado del alternativo antes del examen.
Entrenamiento y calibración separados dentro del periodo anterior a octubre de 2023; excluir etiquetas cuyo horizonte cruza fronteras.
No reentrenar alternativo durante el primer recorrido.
El motor Kotlin puede calcular su histórico de casos como en producción sólo con resultados ya conocidos: código fijo no significa ocultarle información pasada legítima. Registrar este comportamiento y distinguirlo del reentrenamiento del modelo alternativo.
No usar las divisiones porcentuales del prototipo para este protocolo: sustituirlas por fechas explícitas.
El código actual fue diseñado posteriormente a 2023; es una reconstrucción histórica, no evidencia prospectiva auténtica.

## Simulación
Reloj vela a vela. MTF/BTC sólo disponibles tras su cierre, sin normalizaciones aprendidas del futuro.
Guardar decisión de ambos motores, incluyendo NO OPERAR, antes de resolver con velas posteriores.
Mismas ventanas, entrada, objetivo/stop, horizonte y reglas de primer toque para comparación emparejada.
Para ATR usar el motor técnico compartido sólo para definición del objetivo de evaluación; no copiar su score en las variables del alternativo.
Ambos niveles tocados en una vela 5m: neutral/ambiguo.
Datos faltantes: pendiente/no evaluable explícito.
Comparar también cobertura propia; no limitarse a ventanas donde el motor antiguo emite señales.
Deduplicación/cooldown idénticos y registrados. No contar horizontes solapados como ensayos estadísticos independientes.

## Registro y conclusiones
Guardar fecha, versión, parámetros/modelo, variables disponibles, dirección, estimación/confianza separadas, objetivo, stop, plazo, motivo de rechazo y resultado/MFE/MAE.
Informes por año/mes/dirección, cobertura, neutros, pendientes, calibración, Brier, riesgo y costes cuando puedan modelarse.
Si se revisan los tres años completos para diseñar cambios, ninguno es ya una reserva intacta: validar después con datos nuevos, o reservar un tramo antes de inspeccionarlo.
No actualizar la app durante el recorrido ni subir un commit por vela/día simulado.
Hacer cambios técnicos necesarios para ejecutar y corregir errores, sin alterar candidatas tras observar resultados.
No anunciar ganador sin muestra y límites.
La vigilancia Android/remota y el registro prospectivo actuales continúan intactos.
