# CriptoAnálisis Multi 3.8.1 — magnitud y plazo V1

## Alcance

Se añade un evaluador paralelo al predictor Android existente. No se crea un
modelo predictivo entrenado, ni se afirma que mejore su precisión. El resultado
operativo V2 y su calibración siguen intactos. La comparación permite comprobar
qué cambia al medir recorrido y retraso, sobre las mismas señales nuevas.

Referencia revisada: los 59 requisitos originales. Esta ampliación corresponde
a los puntos 18, 20, 25–29, 44, 52 y 53 y conserva la clasificación original del
punto 26. No modifica notificaciones, sonido, vibración, alarmas, Firebase Spark,
Cloudflare Free, selección de activos ni configuración de vigilancia.

## Contrato fijado antes de observar el resultado

Cada señal nueva y cerrada que realmente supera filtros y deduplicación guarda,
en la misma transacción que señal y snapshot, un contrato MAGNITUDE_TIME_V1:

- Dirección y precio de referencia del análisis, con hora real de emisión.
- Objetivo positivo: punto medio del rango porcentual original. Es una referencia
  experimental añadida, no una estimación aprendida a 24 horas ni un precio exacto
  prometido por el modelo. El rango original se conserva.
- Plazo principal 24h, tolerancia 48h, máximo 120h para todos los intervalos.
- Recorrido mínimo aproximado: 75% del objetivo.

Clasificación: HIT si alcanza el objetivo en 24h; APPROXIMATE si alcanza al menos
el 75% dentro de 48h sin cumplir HIT; LATE si alcanza ese mínimo después de 48h y
antes de 120h; FAIL si no lo alcanza en 120h. LATE no cuenta como acierto.
Se conserva PENDING mientras falta plazo o evidencia completa. Una caída previa
no invalida este criterio de recorrido: se registra mediante MAE y sigue afectando
al resultado operativo V2. Estos resultados no equivalen a rentabilidad.

Ejemplo: objetivo +4% BUY, máximo +3% dentro de 48h = APPROXIMATE;
máximo +1% durante 120h = FAIL. SELL es simétrico para caídas. El toque usa
máximos/mínimos de velas, no exige conservar el movimiento al cierre.

## Cronología y datos ausentes

Sólo velas completas de 5 minutos que empiezan después de emitir el contrato.
Se excluye la vela parcial inicial (hasta 5 minutos sin observación). El precio
de referencia sigue siendo el del análisis, no un precio de ejecución garantizado.
Las ventanas se miden desde la emisión, sin recuperar toques anteriores a ella.
No se incluyen velas posteriores a cada plazo; la resolución temporal es 5m.
No se inventa el orden intravela.

El evaluador utiliza únicamente el prefijo continuo desde la primera vela
completa esperada. Un hueco, duplicado o precio inválido detiene la cronología;
no se premia un toque posterior al hueco ni se declara fallo sin cubrir la ventana.
Un objetivo demostrado antes de un hueco posterior sí puede quedar resuelto.
Los contratos pendientes continúan solicitando histórico aunque V2 ya termine
o se desmarque la moneda, mientras se ejecuten ciclos de vigilancia/manuales.

## Persistencia, interfaz y exportación

SQLite V10→V11 añade forecast_criterion y un índice, sin reclasificar señales
antiguas. Conserva parámetros, timestamps de toque, motivo, MFE/MAE y última
revisión. Los resultados finalizados no se reescriben. Una actualización se instala
encima con la misma firma: no desinstalar ni borrar datos.

Rendimiento muestra grupos por cripto/intervalo/resultado y V2 en esos mismos
casos, más los últimos 30 contratos. La ficha muestra el último contrato de la
moneda, identificado por intervalo e ID. Las exportaciones CSV/TXT/RTF completas
incluyen la nueva tabla. No se mezclan muestras antiguas ni se publicita una tasa
de acierto superior por aplicar una regla más permisiva. Las señales solapadas
no son episodios independientes. No hay un segundo predictor para comparar aún.

## Verificación y límites

- Dos pruebas SQLite locales: creación idempotente preservando señales y
  transacción/clave única con rollback.
- 16 pruebas Kotlin añadidas: el ejemplo +4/+3, +1 insuficiente, caídas, SELL,
  límites exactos, retraso, datos futuros, huecos y vela parcial.
- Android CI ejecuta las pruebas unitarias existentes y nuevas y assembleDebug;
  consultar la ejecución de la rama para el resultado real, no asumir que pasó.
- Pendiente prueba física Android: actualizar con firma compatible, generar una
  señal, reiniciar, comprobar el contrato persistido, seguimiento y exportación.
- Verificar en el móvil alarmas/sonido/vibración y vigilancia con pantalla apagada.
  No se han probado físicamente ni se han cambiado Workers o Firebase.

Archivos modificados: AppDatabase.kt, MarketScanRunner.kt, PerformanceActivity.kt,
CryptoDetailActivity.kt, MainActivity.kt, app/build.gradle y android-ci.yml.
Archivos nuevos: ForecastCriterion.kt, ForecastCriterionStore.kt,
ForecastCriterionTest.kt, test_forecast_criterion_schema.py y este documento.
Archivos eliminados: ninguno.

## Comprobación por el usuario

1. Descargar/abrir el proyecto de esta rama en Android Studio y compilar o usar
   la APK de debug de Actions si la firma coincide con la instalada.
2. Instalar como actualización, conservando los datos.
3. Abrir Rendimiento: el histórico V2 debe seguir presente y el criterio nuevo
   comienza vacío; no se simulan contratos pasados.
4. Activar vigilancia o Analizar ahora. Al registrarse una señal nueva debe
   aparecer el contrato en Rendimiento y la ficha, con su objetivo y plazos.
5. Cerrar y reiniciar la app/móvil; comprobar que el contrato sigue existiendo.
6. Exportar el informe completo y localizar CRITERIO MAGNITUD Y PLAZO V1.
7. Tras madurar la ventana, comparar V1 con V2; si falta histórico, debe permanecer
   pendiente. Un evaluador más tolerante no acredita un predictor más preciso.
