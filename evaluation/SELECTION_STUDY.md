# Filtro estadístico experimental — 2026-10-02

Autorización: José Domingo solicita implementar, subir y continuar investigando los métodos propuestos. Se respeta el documento original de los 59 puntos, especialmente 18, 19, 27, 28, 49 y 54. Este módulo es una herramienta de evaluación; no altera Kotlin, persistencia, alertas ni vigilancia local/remota.

Hipótesis inicial: seleccionar señales mediante frecuencias históricas por activo, intervalo, dirección exacta y banda de confianza. Se exige un mínimo de 30 casos resueltos, estimación suavizada Beta(1,1) >= 65% y límite inferior Wilson > 50%. Estos valores definen un ensayo, no objetivos garantizados. Sólo entran casos cuyo horizonte completo de 24h terminó antes de la nueva señal. No se convierten neutrales en aciertos ni se mezclan señales de giro con otras direcciones.

Resultado local con el CSV original: 126 señales, 67 HIT, 58 FAIL, 1 NEUTRAL (53,6%). El filtro selecciona 0: cobertura 0%, precisión indefinida. **No aprobado para producción.** No reducir requisitos después de ver este resultado y presentar el nuevo ensayo como validación independiente.

Comprobaciones locales: cuatro pruebas automáticas para exclusión de futuro, embargo temporal, separación de dirección/neutrales y duplicados. GitHub Actions debe comprobar este estudio y la compilación Android existente. Este cambio no demuestra aumento de precisión ni rentabilidad. Los intervalos son descriptivos: las señales pueden estar correlacionadas. La frecuencia suavizada aún no es una probabilidad calibrada demostrada.

Próximos pasos: ampliar periodos cronológicos y datos prospectivos; exportar variables estructuradas del motor (régimen, ATR, ADX, volumen, MTF, BTC) en la ruta de auditoría; formular pocas hipótesis registradas antes del periodo reservado; medir calibración, cobertura y riesgo/costes. Comparar modelos sencillos y sólo después modelos más complejos. No activar ninguno por un buen resultado sobre el histórico ya inspeccionado.

Cada candidata futura debe registrar versión, variables y reglas, entrenamiento, calibración y evaluación separados cronológicamente con embargo; Brier/calibración, precisión y muestra por dirección y contexto, señales descartadas y límites. La promoción requiere compilación y pruebas, evidencia independiente y conservación de funciones. La app actual continúa siendo la referencia.
