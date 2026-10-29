# Filtro estricto: protocolo anterior al experimento

Solicitud José Domingo: priorizar acierto exacto aunque disminuyan alertas. Fecha simulada29/10/2023 00UTC, no29/10/2026. Archivo oficial existente02/10/2020–02/10/2026; decisiones XRP30m evaluables desde14/06/2021 tras preparar contexto. Primera fase sólo XRP30m, no matriz150 completa ni móvil.

Motor original congelado, umbral81/filtros originales, mismo precio/rango/plazos MAGNITUDE_TIME_V1: exacto24h, aproximado75%48h, tardío48–120h, fallo120h, huecos pendientes. No cambiar objetivos para fabricar precisión. Noticias ausentes neutrales. No modelo2023 aplicado en2020; se reutilizan decisiones as-of y etiquetas Kotlin comprobadas, no precios inventados.

Desarrollo: etiquetas cuyo horizonte completo120h termina estrictamente antes01/04/2023. Validación anterior al examen: emisiones desde01/04/2023 y horizonte120h finalizado estrictamente antes29/10/2023. El resto anterior al examen se embarga. Examen desde29/10/2023 hasta corte archivado. Sin actualizar filtros con resultados del examen. Registrar selección del examen antes de leer sus etiquetas. Histórico ya inspeccionado: exploratorio, NO reserva independiente nueva.

Tres familias fijas, no búsqueda ilimitada: dirección; dirección+régimen original; dirección+régimen+objetivo en[0,1),[1,2),[2,infinito)porciento. Grupos descubiertos sólo en desarrollo/validación, nunca por resultados del examen. Se publican todas las familias sin escoger ganador según examen.

Preselección exige EN CADA fase:100señales maduras,6bloquesUTC de14d,10episodios transitivos120h,95%exactos en todas las señales y90%exactos al escoger primera por episodio sin consultar resultados. Pendientes excluidos del denominador, siempre contados. Episodios por grupo y fase, no declarar independencia estadística.

Admisión estricta añade límite inferior Wilson bilateral, con z equivalente a Bonferroni(alpha0,05 dividido por2*fases*totalgrupos), de al menos90% sobre primeras señales por episodio en cada fase. Este cálculo es una sensibilidad binomial con hipótesis de independencia NO demostrada: NO certifica probabilidad de mercado90%. Si falla, abstenerse. El número de grupos queda congelado antes del examen. No relajar estos límites después de ver resultados ni usar el mejor grupo retrospectivo.

Comparar original sin filtro y cada familia filtrada: señales, cobertura, HIT/APPROXIMATE/LATE/FAIL/PENDING, porcentajes sobre maduros, BUY/SELL, años, episodios por dirección y primera señal; cero señales implica precisión indefinida, no100%. Guardar motivos de rechazo y contrato congelado con hashes. No simulación de rentabilidad en esta fase si no hay señales; nunca sustituir precisión por beneficio.

Sin cambios de producción, modelos prospectivos, alarmas, persistencia o candados del predictor. No certificación90/100% ni garantía. Si no existe evidencia suficiente se informa el fracaso del filtro, sin activar nada.
