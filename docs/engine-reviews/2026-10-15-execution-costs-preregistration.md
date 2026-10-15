# Protocolo adicional: ejecución y costes con datos futuros
Registro: 15 de octubre de 2026. Investigación; no activa operaciones ni cambia el protocolo direccional congelado.

## Pregunta y reserva temporal
¿Las señales congeladas conservan utilidad en una simulación reproducible que use una entrada posterior a su registro y costes explícitos?
Sólo serán admisibles decisiones registradas desde 2026-10-16T00:00:00Z. Los resultados vistos hasta el registro quedan excluidos de esta prueba confirmatoria. No ajustar modelos, umbrales, activo, régimen ni costes según los resultados. Universo: las 15 combinaciones ya registradas y XRP30m, informadas por separado. ADA main continúa como cohorte distinta.
Este documento registra el diseño; la implementación y sus pruebas están pendientes. No se declara una simulación ejecutada.

## Convenciones fijadas antes de evaluar
- Recuperar decisión, recibo durable, hashes y velas oficiales 5m; verificar que el recibo precede a first_open. Sin recibo válido, sin precio oficial o con huecos necesarios: PENDING_DATA, nunca imputar.
- Entrada simulada: apertura oficial de la vela first_open posterior a escritura durable. Mantener los niveles target/stop calculados a partir de entry y target_pct del evento congelado. No cambiar las etiquetas HIT/FAIL/NEUTRAL existentes.
- Si al abrir la operación ya se ha cruzado un nivel registrado, marcar NOT_EXECUTABLE_AT_REGISTERED_LEVELS y mostrar su frecuencia; no adjudicar un beneficio retrospectivo.
- Salida al primer nivel tocado. Si una apertura posterior salta el stop, usar esa apertura adversa; para un objetivo saltado, limitar el precio al objetivo. Si ambos niveles se tocan dentro de la misma vela sin orden identificable, informar intervalo stop/objetivo; no inventar trayectoria ni beneficio puntual.
- Si no se toca ningún nivel, salida al cierre de la última vela completa del horizonte. No evaluar antes de disponer de la ventana completa exigida por el protocolo; conservar pendiente ante huecos.
- Caso principal: comisión hipotética 10 puntos básicos por lado y deslizamiento adverso 10 puntos básicos por lado. Aplicar deslizamiento al precio de cada fill (BUY mayor, SELL menor); comisión sobre cada importe negociado. Sensibilidades fijas: mismo fee y deslizamiento 0 / 20 puntos básicos por lado. No escoger retrospectivamente el escenario favorable.
- Capital nocional constante, sin apalancamiento ni reinversión. Operaciones SELL se mostrarán como posiciones cortas hipotéticas separadas: no se supone que eToro permita ejecutarlas; financiación y préstamo no modelados. Los costes son supuestos de sensibilidad, no tarifas verificadas de un broker.
- Por activo y motor, admitir cronológicamente sólo la primera señal mientras esté libre; bloquear nuevas entradas hasta horizon_end de la señal admitida, aun si el primer toque ocurrió antes. Resolver empates por first_open, recorded_at e id. Decidir la admisión sin consultar resultados. Las señales omitidas permanecen en el registro y en las métricas direccionales originales.
- Original y alternativo conservan sus propias aceptaciones. Comparación pareada estricta sólo para el mismo id aceptado por ambos, con la misma entrada y costes. Informar también la cobertura y el resultado de la política completa; no presentar cohortes distintas como comparación pareada.

## Salidas y condiciones para interpretar
Guardar por operación: identificador, hash del evento/velas/recibo, entrada/salida simuladas, motivo, coste, retorno nocional, intervalo de ambigüedad, MFE/MAE, régimen conocido al registrar y estado.
Publicar cobertura, huecos, no ejecutables, aciertos/fallos/neutros originales y retornos simulados separados, por BUY/SELL, moneda/plazo y bloques de calendario. No confundir MFE de toda la ventana con beneficio realizable.
No declarar superioridad a partir de pocas operaciones o ventanas dependientes; conservar el control de selección múltiple de los protocolos existentes. Revisión principal no anterior al plazo de 180 días del seguimiento adicional; los resúmenes intermedios son descriptivos. Cualquier modificación posterior se registra como un protocolo distinto con nueva reserva futura.
No desplegar en Android un motor/filtro por esta simulación sin ventaja prospectiva independiente, estabilidad por regímenes y revisión de incertidumbre. Un retorno hipotético positivo no demuestra rentabilidad ejecutable.

## Siguiente trabajo
Implementar la transformación como herramienta independiente de investigación, probar entradas posteriores al recibo, gaps, saltos, doble toque, costes y bloqueo por activo. Verificarla en Actions antes de publicar cualquier resultado. No repetir la matriz150 ni reentrenar modelos.

## Implementación comprobada — 15/10/2026 08:40 Madrid

El núcleo evaluation/execution_costs.py y sus 15 pruebas ya están publicados en c864a9b07979c60ff7b9bfd6844dd969ac732fe1. Actions 38693622444 supera las 85 pruebas del conjunto. Las secciones anteriores conservan el diseño registrado antes de implementar: no se han cambiado la reserva futura, costes, selección ni criterios.

Uso para la cohorte seleccionada (los mismos argumentos admiten journal/recibos/lock XRP por separado):

```bash
python3 evaluation/execution_costs.py \\
  --events evaluation/selected-prospective/events-2026-10.jsonl \\
  --receipts evaluation/selected-prospective/durability-receipts.json \\
  --freeze-lock evaluation/selected-prospective/freeze-lock.json \\
  --data selected-data --output execution-costs.json
```

La ejecución exige manifest.json del recolector nativo oficial y verifica los CSV5m por checksum. Devuelve estados explícitos, sensibilidades por operación, comparaciones estrictamente pareadas sólo entre operaciones admitidas del mismo id, régimen del snapshot cuando existe y MFE/MAE direccionales originales claramente separados de PnL. No altera diarios ni modelos ni emite órdenes.

No está conectada a un cálculo periódico de resultados de costes y todavía no existe una muestra de mercado posterior al inicio reservado. Los informes estadísticos por bloque/regímenes y la operación periódica de esta simulación siguen pendientes. La validación actual comprueba la herramienta, no una ventaja predictiva o rentabilidad ejecutable.
