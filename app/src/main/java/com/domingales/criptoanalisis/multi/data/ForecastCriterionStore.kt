package com.domingales.criptoanalisis.multi.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.domingales.criptoanalisis.multi.domain.*

/** Contratos prospectivos independientes. No reclasifica el histórico anterior. */
class ForecastCriterionStore(private val database: AppDatabase) {
    companion object {
        fun createSchema(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS forecast_criterion(
                signal_uid TEXT PRIMARY KEY,profile TEXT NOT NULL,symbol TEXT NOT NULL,timeframe TEXT NOT NULL,
                issued_ts INTEGER NOT NULL,entry REAL NOT NULL,direction TEXT NOT NULL,target_pct REAL NOT NULL,
                horizon_ms INTEGER NOT NULL,grace_ms INTEGER NOT NULL,expiry_ms INTEGER NOT NULL,
                minimum_fraction REAL NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,
                first_target_ts INTEGER,first_approximate_ts INTEGER,mfe REAL DEFAULT 0,mae REAL DEFAULT 0,checked_ts INTEGER)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_forecast_pending ON forecast_criterion(status,symbol,issued_ts)")
        }

        fun register(db: SQLiteDatabase, uid: String, analysis: AnalysisResult) {
            // El rango original no fija un objetivo puntual: se declara su centro
            // como referencia experimental nueva, antes de observar el resultado.
            val target = (analysis.forecastLowPct + analysis.forecastHighPct) / 2.0
            if (!analysis.closedCandleEvaluation || !analysis.isSignal || !target.isFinite() || target <= 0) return
            val contract = ForecastCriterion.Contract(analysis.indicators.price, analysis.direction, analysis.timestamp, target)
            val values = ContentValues().apply {
                put("signal_uid", uid); put("profile", ForecastCriterion.PROFILE)
                put("symbol", analysis.symbol); put("timeframe", analysis.timeframe)
                put("issued_ts", contract.issuedAt); put("entry", contract.entry); put("direction", contract.direction.name)
                put("target_pct", contract.targetPct); put("horizon_ms", contract.horizonMillis)
                put("grace_ms", contract.graceMillis); put("expiry_ms", contract.expiryMillis)
                put("minimum_fraction", contract.minimumFraction); put("status", "PENDING"); put("reason", "WAITING_CLOSED_5M")
            }
            db.insertOrThrow("forecast_criterion", null, values)
        }
    }

    fun pendingStarts(): Map<String, Long> {
        val result = linkedMapOf<String, Long>()
        database.readableDatabase.rawQuery("SELECT symbol,MIN(issued_ts) FROM forecast_criterion WHERE status='PENDING' GROUP BY symbol", null).use { c ->
            while (c.moveToNext()) result[c.getString(0)] = c.getLong(1)
        }
        return result
    }

    fun update(histories: Map<String, List<Candle>>, now: Long) {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.rawQuery("SELECT signal_uid,symbol,entry,direction,issued_ts,target_pct,horizon_ms,grace_ms,expiry_ms,minimum_fraction FROM forecast_criterion WHERE status='PENDING'", null).use { c ->
                while (c.moveToNext()) {
                    val contract = ForecastCriterion.Contract(c.getDouble(2), Direction.valueOf(c.getString(3)), c.getLong(4), c.getDouble(5), c.getLong(6), c.getLong(7), c.getLong(8), c.getDouble(9))
                    val result = ForecastCriterion.evaluate(contract, histories[c.getString(1)].orEmpty(), now)
                    val v = ContentValues().apply {
                        put("status", result.status.name); put("reason", result.reason); put("checked_ts", now)
                        put("mfe", result.mfe); put("mae", result.mae)
                        if (result.firstTargetAt == null) putNull("first_target_ts") else put("first_target_ts", result.firstTargetAt)
                        if (result.firstApproximateAt == null) putNull("first_approximate_ts") else put("first_approximate_ts", result.firstApproximateAt)
                    }
                    db.update("forecast_criterion", v, "signal_uid=? AND status='PENDING'", arrayOf(c.getString(0)))
                }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun report(symbol: String? = null, limit: Int = 30): List<String> {
        val rows = mutableListOf<String>()
        val where = if (symbol == null) "" else " WHERE f.symbol=?"
        val args = symbol?.let { arrayOf(it) }
        database.readableDatabase.rawQuery("""SELECT f.symbol,f.timeframe,f.status,COUNT(*),
            SUM(CASE WHEN s.status='HIT' THEN 1 ELSE 0 END),SUM(CASE WHEN s.status='FAIL' THEN 1 ELSE 0 END)
            FROM forecast_criterion f JOIN signal s ON s.uid=f.signal_uid$where
            GROUP BY f.symbol,f.timeframe,f.status ORDER BY f.symbol,f.timeframe,f.status""", args).use { c ->
            while (c.moveToNext()) rows += "${c.getString(0)} ${c.getString(1)} • ${label(c.getString(2))}: ${c.getInt(3)} • En esos mismos casos, V2: ${c.getInt(4)} aciertos / ${c.getInt(5)} fallos (resto pendientes o neutros)"
        }
        database.readableDatabase.rawQuery("""SELECT f.signal_uid,f.direction,f.target_pct,f.status,f.mfe,f.mae,s.status
            FROM forecast_criterion f JOIN signal s ON s.uid=f.signal_uid$where ORDER BY f.issued_ts DESC LIMIT ${limit.coerceIn(1, 100)}""", args).use { c ->
            while (c.moveToNext()) rows += "${c.getString(0)}\n${c.getString(1)} • objetivo ${"%.2f".format(c.getDouble(2))}% en 24h; mínimo ${"%.2f".format(c.getDouble(2) * 0.75)}% en 48h; cierre máximo 120h\n${label(c.getString(3))} • V2: ${c.getString(6)} • MFE ${"%.2f".format(c.getDouble(4))}% / MAE ${"%.2f".format(c.getDouble(5))}% hasta la revisión que resuelve el caso"
        }
        return rows
    }

    private fun label(status: String): String = when (status) {
        "HIT" -> "ACIERTO EN PLAZO"
        "APPROXIMATE" -> "ACIERTO APROXIMADO"
        "LATE" -> "FUERA DE PLAZO (no cuenta como acierto)"
        "FAIL" -> "FALLO"
        else -> "PENDIENTE (plazo o datos)"
    }
}
