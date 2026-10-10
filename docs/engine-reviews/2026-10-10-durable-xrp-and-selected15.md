# Activación durable de XRP30m y 15 combinaciones destacadas

Revisión: 10 de octubre de 2026, 08:35 Europe/Madrid.

## Resultado operativo

La recogida XRP30m permanece activa con el modelo congelado. La ejecución 38030602407 recuperó las cinco decisiones existentes, mantuvo el mismo modelo/protocolo y terminó correctamente. No añadió una decisión porque no había una vela 30m nueva distinta de la ya registrada. Hay 5 decisiones, 2 señales originales SELL pendientes, 0 señales alternativas y ningún resultado vencido. La primera ventana finaliza el 10 de octubre a las 09:04:59 Europe/Madrid.

La recogida separada de las 15 combinaciones ha quedado activada en 38030613558. Recuperó y verificó los 15 modelos prospectivos congelados; ARB-1d también es prospectivamente evaluable, aunque no lo fuera para el examen histórico iniciado en 2023. Se registraron 15 decisiones durables, una por combinación, antes de sus velas de resultado. Sólo ATOM-1d produjo una señal original, pendiente; el alternativo no produjo señales al umbral 0,65. No hay resultados vencidos ni precisión calculable.

Las dos recogidas usan una única cola de escritura para evitar conflictos. El rechazo de 38030359966 fue una protección correcta ante un cambio concurrente de la rama mientras se publicaban las herramientas de las 15 combinaciones: no se forzó ni sobrescribió la rama. Se añadió reintento sólo para el mismo avance rápido sin cambios externos.

## Estado cuantitativo

- XRP30m: 5 decisiones; original 2 señales pendientes; alternativo 0 señales; 0 HIT, 0 FAIL, 0 NEUTRAL.
- 15 combinaciones: 15 decisiones; original 1 señal ATOM-1d pendiente; alternativo 0 señales; 0 HIT, 0 FAIL, 0 NEUTRAL.
- ADA1h main: 36 decisiones; 2 señales; 2 HIT; 0 FAIL; 0 NEUTRAL.
- Pruebas: 70 pruebas Python correctas. Android 38030613499 correcto.

## Límites

No hay una mejora predictiva demostrada. Las primeras decisiones son observaciones prospectivas, no resultados. Los motores, umbrales, protocolos y modelos permanecen congelados; no se rellenan decisiones omitidas ni se reentrena con datos de la validación. La PR #9 sigue abierta y no se ha fusionado. Android, alarmas, identidad visual, persistencia de la app y vigilancia híbrida no se han modificado.

Siguiente paso: resolver únicamente ventanas vencidas, acumular Brier/calibración de todas las puntuaciones y resultados por señal/episodio; después evaluar incertidumbre por bloques, regímenes, BUY/SELL y costes antes de considerar cualquier cambio de producción.
