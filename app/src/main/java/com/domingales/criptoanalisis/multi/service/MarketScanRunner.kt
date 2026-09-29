package com.domingales.criptoanalisis.multi.service

import android.content.Context
import android.os.PowerManager
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.data.MarketRepository
import com.domingales.criptoanalisis.multi.domain.Regime
import com.domingales.criptoanalisis.multi.util.Prefs
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/** Ejecuta un ciclo finito compartido por servicio local, FCM, respaldo y acción manual. */
object MarketScanRunner {
    private val coordinator = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)

    fun start(context: Context, force: Boolean = false, origin: String = "PROGRAMADO", requestedAt: Long = 0L, finished: (Boolean) -> Unit): Boolean {
        val app = context.applicationContext
        val db = AppDatabase.get(app)
        if (!Prefs.tryAcquireCycle(app, origin, force)) {
            db.log("SYSTEM", "CYCLE_SKIPPED", "$origin omitido: ciclo activo o activación equivalente demasiado próxima")
            return false
        }
        if (!running.compareAndSet(false, true)) {
            Prefs.releaseCycle(app, true)
            db.log("SYSTEM", "CYCLE_SKIPPED", "$origin omitido: coordinador ocupado")
            return false
        }
        coordinator.execute {
            var retry = false
            var wakeLock: PowerManager.WakeLock? = null
            try {
                wakeLock = app.getSystemService(PowerManager::class.java)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CriptoAnalisisMulti:scan").apply {
                        setReferenceCounted(false)
                        acquire(10 * 60_000L)
                    }
                runCycle(app, force, origin, requestedAt)
            } catch (t: Throwable) {
                retry = true
                AppDatabase.get(app).log("SYSTEM", "ERROR", "Ciclo abortado: ${t.message ?: t.javaClass.simpleName}")
            } finally {
                if (wakeLock?.isHeld == true) wakeLock.release()
                Prefs.releaseCycle(app, !retry)
                running.set(false)
                finished(retry)
            }
        }
        return true
    }

    private fun runCycle(context: Context, force: Boolean, origin: String, requestedAt: Long) {
        if (!force && !Prefs.watching(context)) return
        val db = AppDatabase.get(context)
        val repo = MarketRepository(context)
        val symbols = Prefs.watchlist(context).sorted()
        db.syncConfiguration(context)
        deliverPendingAlerts(context, db)
        deliverPendingExplosionAlerts(context, db)
        val delay = if (requestedAt > 0L) " • retraso ${(System.currentTimeMillis() - requestedAt).coerceAtLeast(0L) / 1000L}s" else ""
        db.log("SYSTEM", "CYCLE", "Inicio $origin$delay • ${symbols.size} criptos • modo ${if (Prefs.closedCandleMode(context)) "VELA CERRADA" else "INTRAVELA"}")

        var btcRegime: Regime? = null
        try {
            btcRegime = repo.quickRegime("BTC", "4h")
        } catch (t: Throwable) {
            db.log("BTC", "CONTEXT", "Contexto BTC no disponible: ${t.message}")
        }

        val workers = Executors.newFixedThreadPool(2)
        try {
            val futures = mutableListOf<Future<*>>()
            for (symbol in symbols) {
                val timeframes = Prefs.symbolTimeframes(context, symbol)
                    .ifEmpty { setOf(Prefs.timeframe(context)) }
                    .sortedBy(::tfOrder)
                for (tf in timeframes) {
                    futures += workers.submit {
                        try {
                            repo.analyzeCycle(symbol, tf, Prefs.symbolThreshold(context, symbol), btcRegime).forEach { analysis ->
                                val saved = db.saveAnalysis(analysis, analysis.closedCandleEvaluation)
                                if (!saved) return@forEach
                                val state = when {
                                    analysis.isSignal -> "SIGNAL"
                                    analysis.isPreSignal -> "PRESIGNAL"
                                    else -> "EVAL"
                                }
                                val phase = if (analysis.closedCandleEvaluation) "CIERRE" else "INTRAVELA"
                                db.log(symbol, state, "$tf $phase score=${analysis.score} conf=${analysis.confidence} prob=${analysis.probability ?: "N/D"} dir=${analysis.direction} ${if (analysis.rejectionReason.isNotBlank()) "rechazo=${analysis.rejectionReason}" else "filtros=OK"}")
                                if (analysis.closedCandleEvaluation && Prefs.explosionDetectorEnabled(context)) {
                                    val assessment = repo.explosionAssessment(analysis)
                                    if (assessment.confirmed) {
                                        val insertedExplosion = db.insertExplosionIfNew(assessment)
                                        if (insertedExplosion.inserted) {
                                            db.log(symbol, "EXPLOSION", "${insertedExplosion.uid} • 5/5 • volumen ${"%.2f".format(assessment.volumeRatio)}x • ATR ${"%.2f".format(assessment.atrExpansionRatio)}x • ADX ${"%.1f".format(assessment.adx)}")
                                            val pending = db.pendingExplosionAlerts().firstOrNull { it.uid == insertedExplosion.uid }
                                            if (pending != null) try {
                                                NotificationHelper.explosion(context, pending)
                                                db.markExplosionAlertSent(insertedExplosion.uid)
                                            } catch (t: Throwable) {
                                                db.markExplosionAlertFailed(insertedExplosion.uid, t.message ?: t.javaClass.simpleName)
                                                db.log(symbol, "ERROR", "Notificación de explosión pendiente ${insertedExplosion.uid}: ${t.message}")
                                            }
                                        }
                                    }
                                }
                                if (analysis.isSignal) {
                                    val inserted = db.insertSignalAndAlertIfNew(analysis, Prefs.cooldownMinutes(context))
                                    if (inserted.inserted) {
                                        db.log(symbol, "ALERT", "${inserted.uid} • ${analysis.direction} ${analysis.confidence}% • riesgo ${analysis.risk}")
                                        try {
                                            NotificationHelper.signal(context, analysis)
                                            db.markAlertSent(inserted.uid)
                                        } catch (t: Throwable) {
                                            db.markAlertFailed(inserted.uid, t.message ?: t.javaClass.simpleName)
                                            db.log(symbol, "ERROR", "Notificación pendiente ${inserted.uid}: ${t.message}")
                                        }
                                    } else db.log(symbol, "DUPLICATE", "$tf señal equivalente/cooldown; no se alerta")
                                }
                            }
                        } catch (t: Throwable) {
                            db.log(symbol, "ERROR", "$tf ${t.message ?: t.javaClass.simpleName}")
                        }
                    }
                }
            }
            futures.forEach { try { it.get() } catch (_: Throwable) { } }
        } finally {
            workers.shutdownNow()
        }

        val prices = mutableMapOf<String, Double>()
        val explosionStarts = db.pendingExplosionStarts()
        val priceSymbols = (symbols + db.priceAlarms().filter { it.status == "ACTIVE" }.map { it.symbol } + explosionStarts.keys).distinct()
        for (symbol in priceSymbols) {
            try { prices[symbol] = repo.currentPrice(symbol) }
            catch (t: Throwable) { db.log(symbol, "FOLLOWUP", "Precio no disponible: ${t.message}") }
        }
        val now = System.currentTimeMillis()
        val histories = mutableMapOf<String, List<com.domingales.criptoanalisis.multi.domain.Candle>>()
        val followupStarts = db.pendingFollowupStarts().toMutableMap()
        explosionStarts.forEach { (symbol, start) -> followupStarts[symbol] = minOf(followupStarts[symbol] ?: start, start) }
        followupStarts.forEach { (symbol, start) ->
            try { histories[symbol] = repo.followupCandles(symbol, start, now) }
            catch (t: Throwable) { db.log(symbol, "FOLLOWUP", "Velas de seguimiento no disponibles: ${t.message}") }
        }
        try { db.updateFollowups(prices, histories, now) }
        catch (t: Throwable) { db.log("SYSTEM", "FOLLOWUP", t.message ?: "Error en seguimiento") }
        try { db.updateExplosionOutcomes(prices, histories, now) }
        catch (t: Throwable) { db.log("SYSTEM", "EXPLOSION_FOLLOWUP", t.message ?: "Error en seguimiento de explosiones") }
        try {
            db.evaluatePriceAlarms(prices).forEach { alarm ->
                NotificationHelper.priceAlarm(context, alarm)
                db.log(alarm.symbol, "PRICE_ALARM", "${alarm.uid} • ${alarm.condition} ${alarm.target} • precio ${alarm.lastPrice}")
            }
        } catch (t: Throwable) { db.log("SYSTEM", "PRICE_ALARM", t.message ?: "Error evaluando alarmas") }
        db.log("SYSTEM", "CYCLE", "Fin $origin • próxima ventana local ~${MonitoringScheduler.effectiveLocalMinutes(context)} min")
    }

    private fun deliverPendingAlerts(context: Context, db: AppDatabase) {
        db.pendingAlerts().forEach { pending ->
            try {
                NotificationHelper.signal(context, pending)
                db.markAlertSent(pending.uid)
                db.log(pending.symbol, "ALERT_RECOVERED", "${pending.uid} • notificación recuperada desde outbox")
            } catch (t: Throwable) {
                db.markAlertFailed(pending.uid, t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private fun deliverPendingExplosionAlerts(context: Context, db: AppDatabase) {
        db.pendingExplosionAlerts().forEach { pending ->
            try {
                NotificationHelper.explosion(context, pending)
                db.markExplosionAlertSent(pending.uid)
                db.log(pending.symbol, "EXPLOSION_ALERT_RECOVERED", "${pending.uid} • notificación recuperada desde outbox independiente")
            } catch (t: Throwable) {
                db.markExplosionAlertFailed(pending.uid, t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private fun tfOrder(tf: String) = when (tf) { "15m" -> 1; "30m" -> 2; "1h" -> 3; "4h" -> 4; "1d" -> 5; else -> 9 }
}
