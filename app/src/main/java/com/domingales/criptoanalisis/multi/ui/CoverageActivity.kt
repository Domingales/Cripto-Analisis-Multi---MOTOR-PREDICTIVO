package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView

class CoverageActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val s = ScrollView(this); val r = Ui.root(this); s.addView(r)
        r.addView(Ui.title(this, "Mapa verificable de los 59 puntos"))
        r.addView(Ui.subtitle(this, "Esta pantalla indica dónde se comprueba cada capacidad. No sustituye al diagnóstico: enlaza el diseño con funciones visibles de la APK."))
        val groups = listOf(
            "NÚCLEO MULTCRIPTO" to listOf(
                "1. Base útil anterior" to "Selector, intervalos, análisis, gráfico, alarmas de precio, exportación e historial.",
                "2. Central multcripto" to "Cada activo se procesa y persiste de forma independiente.",
                "3. Mis Criptos" to "Catálogo Binance buscable, selección múltiple y favoritas.",
                "4. Configuración por cripto" to "Intervalos, umbral, compra/venta, sonido y vibración propios.",
                "5. Repositorio de mercado" to "Una capa central obtiene y valida mercado.",
                "6. Datos históricos + precio actual" to "OHLCV para análisis y ticker para seguimiento."
            ),
            "ANÁLISIS TÉCNICO" to listOf(
                "7. Tendencia EMA" to "EMA20/50/200, orden, pendiente y posición del precio.",
                "8. RSI avanzado" to "Valor, pendiente, zonas y divergencias.",
                "9. ADX / DMI" to "ADX, +DI y -DI distinguen fuerza y dirección.",
                "10. ATR / volatilidad" to "ATR%, riesgo y rango de movimiento esperado.",
                "11. Volumen" to "Ratio, expansión y confirmación de ruptura.",
                "12. Soportes/resistencias" to "Zonas, contactos, fuerza y ruptura.",
                "13. Estructura" to "BULLISH/BEARISH/SIDEWAYS/MIXED a partir de swings.",
                "14. Multitemporal" to "15m, principal, 4h y 1d con alineación.",
                "15. Régimen" to "Tendencia, rango, volatilidad, breakout, giro con evidencia y estado MIXED neutral.",
                "16. Contexto BTC" to "BTC 4h modifica el riesgo de altcoins."
            ),
            "DECISIÓN Y PREDICCIÓN" to listOf(
                "17. Indicador Maestro" to "Desglose visible de 10 componentes.",
                "18. Score ≠ probabilidad" to "Score técnico, confianza y probabilidad calibrada separados.",
                "19. Recomendaciones" to "BUY, SELL, WAIT y giros.",
                "20. Predicción" to "Rango esperado, horizonte, invalidación y riesgo.",
                "21. Cadena de señal" to "Datos → análisis → filtros → señal → alerta.",
                "22. Alerta transaccional" to "Señal, snapshot, seguimientos, evento y outbox se confirman juntos.",
                "23. UID único" to "Símbolo + timeframe + cierre + dirección.",
                "24. Historial independiente" to "Cada señal conserva cripto y timeframe."
            ),
            "APRENDIZAJE Y SEGUIMIENTO" to listOf(
                "25. Seguimiento" to "Snapshot reproducible + persistencia con horizontes adaptados a 15m, 30m, 1h, 4h o 1d.",
                "26. Resultado" to "Resultado operativo por primer toque y persistencia separada; velas 5m posteriores, MFE y MAE.",
                "27. Casos comparables" to "Resultados exactos posteriores a señal y del mismo lado BUY/SELL; los heredados quedan excluidos.",
                "28. Casos mínimos" to "Probabilidad solo desde 10 comparables resueltos; antes figura sin calibrar.",
                "29. Autoevaluación" to "Rendimiento exacto total, por cripto y timeframe; legado excluido visible."
            ),
            "ANDROID / ALERTAS" to listOf(
                "30. Foreground Service" to "Vigilancia Android nativa.",
                "31. Pantalla apagada" to "JobScheduler recuperable + activación remota híbrida opcional.",
                "32. Reinicio" to "BOOT_COMPLETED restaura el trabajo sin iniciar un FGS prohibido.",
                "33. Notificaciones" to "Cripto, dirección, intervalo, precio, riesgo, probabilidad/casos y canales de sonido/vibración configurables.",
                "34. Anti-spam" to "UID, cooldown y mejora mínima para repetir.",
                "35. Tipos de oportunidad" to "Tendencia, ruptura, giro y volumen."
            ),
            "INTERFAZ Y AUDITORÍA" to listOf(
                "36. Dashboard" to "Variación, última alerta y orden por confianza, cambio, nombre, señal o alerta.",
                "37. Semáforo" to "Verde/rojo/amarillo/blanco y morado para señal en seguimiento.",
                "38. Ficha individual" to "Gráfico, variación, EMA/DMI/ATR/volumen, S/R, MTF, score, histórico y seguimiento.",
                "39. Explicación" to "Razones y evidencias visibles.",
                "40. Menú completo" to "Hamburguesa global configurable a izquierda/derecha con acceso a todos los módulos.",
                "41. Oportunidades" to "Ranking por prioridad y confianza.",
                "42. Diagnóstico" to "Internet/API, vigilancia, últimas revisiones, análisis del día, contadores y estado por cripto.",
                "43. No-señal" to "Motivo de rechazo persistido y mostrado."
            ),
            "ARQUITECTURA / DATOS" to listOf(
                "44. Base de datos transaccional" to "Activos, configuración, velas, análisis, snapshots, señales, resultados, alertas/outbox y diagnóstico.",
                "45. Paralelismo controlado" to "Coordinador + pool limitado de workers.",
                "46. Caché" to "Velas persistentes, EMA/RSI incrementales e indicadores reutilizados por cierre.",
                "47. Cerrada / intravela" to "Evaluación dual: abierta sólo PRESEÑAL y última cerrada apta para SIGNAL.",
                "48. Estados" to "EVALUATION, PRESIGNAL, SIGNAL, ALERT y RESULT.",
                "49. Motor reproducible" to "La decisión crítica no depende de IA externa.",
                "50. Noticias" to "Módulo separado y peso auxiliar.",
                "51. Seguridad API" to "Timeout, retry, backoff, frescura y validación.",
                "52. Migraciones" to "Conservación de datos al subir versión.",
                "53. Exportación" to "Global por flujo en segundo plano y por ficha: portapapeles, compartir, TXT, CSV/Excel y RTF/Word.",
                "54. Backtest" to "Walk-forward MTF/BTC/aprendizaje sin futuro, ventana adaptada, primer toque, cooldown y BUY/SELL.",
                "55. No forzar señales" to "WAIT y filtros se mantienen aunque haya score alto.",
                "56. Arquitectura modular" to "data/domain/service/ui/util.",
                "57. Flujo multcripto completo" to "Procesa, persiste, deduplica, alerta y sigue.",
                "58. Sin estado global de cripto" to "Configuración y análisis por símbolo/timeframe.",
                "59. Proyecto nuevo" to "CriptoAnálisis Multi 3.8.0, núcleo separado de la app monocripto."
            )
        )
        groups.forEach { (name, points) ->
            r.addView(Ui.section(this, name))
            points.forEach { (title, detail) ->
                val c = Ui.card(this); c.addView(Ui.text(this, "✓ $title", 14f, true)); c.addView(Ui.text(this, detail, 12f)); r.addView(c)
            }
        }
        setContentView(s)
    }
}
