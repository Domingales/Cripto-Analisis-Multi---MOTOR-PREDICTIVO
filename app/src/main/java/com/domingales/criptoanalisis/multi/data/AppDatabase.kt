package com.domingales.criptoanalisis.multi.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.domingales.criptoanalisis.multi.domain.*
import com.domingales.criptoanalisis.multi.util.Prefs
import org.json.JSONArray
import java.io.StringWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(context, "crypto_multi.db", null, 10) {
    override fun onCreate(db: SQLiteDatabase) = createSchema(db)

    private fun createSchema(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS analysis(id INTEGER PRIMARY KEY AUTOINCREMENT,symbol TEXT,timeframe TEXT,ts INTEGER,candle_close INTEGER,price REAL,change_pct REAL,direction TEXT,score INTEGER,confidence INTEGER,probability INTEGER,presignal INTEGER,signal INTEGER,state TEXT,rejection TEXT,explanation TEXT,rsi REAL,adx REAL,plus_di REAL,minus_di REAL,atr REAL,atr_pct REAL,ema20 REAL,ema50 REAL,ema200 REAL,ema20_slope REAL,ema50_slope REAL,volume_ratio REAL,volume_expansion INTEGER DEFAULT 0,support REAL,resistance REAL,support_low REAL,support_high REAL,resistance_low REAL,resistance_high REAL,regime TEXT,structure TEXT,mtf_score INTEGER,mtf_summary TEXT,risk TEXT,forecast_low REAL,forecast_high REAL,invalidation REAL,trend_score INTEGER,momentum_score INTEGER,volume_score INTEGER,structure_score INTEGER,sr_score INTEGER,mtf_component INTEGER,volatility_score INTEGER,historical_score INTEGER,btc_score INTEGER,fundamental_score INTEGER,comparable_cases INTEGER,hist_probability INTEGER,stat_confidence TEXT,data_quality TEXT DEFAULT 'COMPLETE',missing_data TEXT DEFAULT '')""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS signal(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE,symbol TEXT,timeframe TEXT,ts INTEGER,candle_close INTEGER,price REAL,direction TEXT,score INTEGER,confidence INTEGER,probability INTEGER,atr REAL,risk TEXT,status TEXT DEFAULT 'PENDING',outcome_quality TEXT DEFAULT 'EXACT_FIRST_TOUCH_5M_V2',outcome_reason TEXT DEFAULT '',outcome_resolved_ts INTEGER,outcome_window_min INTEGER DEFAULT 1440,threshold_pct REAL DEFAULT 0,target_price REAL,stop_price REAL,persistence_status TEXT DEFAULT 'PENDING',persistence_quality TEXT DEFAULT 'ADAPTIVE_V2',legacy_status TEXT,mfe REAL DEFAULT 0,mae REAL DEFAULT 0,last_price REAL,updated_ts INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS followup(id INTEGER PRIMARY KEY AUTOINCREMENT,signal_uid TEXT,horizon_min INTEGER,due_ts INTEGER,checked_ts INTEGER,market_ts INTEGER,price REAL,move_pct REAL,status TEXT DEFAULT 'PENDING',UNIQUE(signal_uid,horizon_min))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS persistence_followup(id INTEGER PRIMARY KEY AUTOINCREMENT,signal_uid TEXT,horizon_min INTEGER,due_ts INTEGER,checked_ts INTEGER,market_ts INTEGER,price REAL,move_pct REAL,status TEXT DEFAULT 'PENDING',profile TEXT DEFAULT 'ADAPTIVE_V2',UNIQUE(signal_uid,horizon_min))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS diagnostic(id INTEGER PRIMARY KEY AUTOINCREMENT,ts INTEGER,symbol TEXT,type TEXT,message TEXT)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS alert_event(id INTEGER PRIMARY KEY AUTOINCREMENT,signal_uid TEXT UNIQUE,ts INTEGER,symbol TEXT,kind TEXT,message TEXT)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS candle_cache(symbol TEXT,timeframe TEXT,open_time INTEGER,open REAL,high REAL,low REAL,close REAL,volume REAL,close_time INTEGER,PRIMARY KEY(symbol,timeframe,open_time))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS signal_snapshot(signal_uid TEXT PRIMARY KEY,symbol TEXT,timeframe TEXT,ts INTEGER,candle_close INTEGER,price REAL,direction TEXT,score INTEGER,confidence INTEGER,probability INTEGER,risk TEXT,rsi REAL,rsi_slope REAL,rsi_bull_div INTEGER,rsi_bear_div INTEGER,adx REAL,plus_di REAL,minus_di REAL,atr REAL,atr_pct REAL,ema20 REAL,ema50 REAL,ema200 REAL,ema20_slope REAL,ema50_slope REAL,volume_ratio REAL,volume_expansion INTEGER,support REAL,resistance REAL,support_low REAL,support_high REAL,resistance_low REAL,resistance_high REAL,support_touches INTEGER,resistance_touches INTEGER,support_strength INTEGER,resistance_strength INTEGER,structure TEXT,regime TEXT,breakout_up INTEGER,breakout_down INTEGER,false_breakout INTEGER,expected_move_atr REAL,mtf_score INTEGER,mtf_summary TEXT,btc_regime TEXT,trend_score INTEGER,momentum_score INTEGER,volume_score INTEGER,structure_score INTEGER,sr_score INTEGER,mtf_component INTEGER,volatility_score INTEGER,historical_score INTEGER,btc_score INTEGER,fundamental_score INTEGER,fundamental_headlines INTEGER,fundamental_positive INTEGER,fundamental_negative INTEGER,fundamental_summary TEXT,comparable_cases INTEGER,calibration_wins INTEGER,calibration_losses INTEGER,calibration_neutral INTEGER,hist_probability INTEGER,stat_confidence TEXT,forecast_low REAL,forecast_high REAL,invalidation REAL,decision_threshold INTEGER,closed_candle INTEGER,allow_buy INTEGER,allow_sell INTEGER,explanation TEXT,missing_data TEXT DEFAULT '',snapshot_quality TEXT DEFAULT 'COMPLETE')""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS alert_outbox(signal_uid TEXT PRIMARY KEY,created_ts INTEGER,status TEXT DEFAULT 'PENDING',attempts INTEGER DEFAULT 0,last_error TEXT DEFAULT '',sent_ts INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS indicator_cache(symbol TEXT,timeframe TEXT,candle_close INTEGER,payload TEXT,updated_ts INTEGER,PRIMARY KEY(symbol,timeframe,candle_close))""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS crypto_asset(symbol TEXT PRIMARY KEY,name TEXT,enabled INTEGER,updated_ts INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS watch_config(symbol TEXT PRIMARY KEY,timeframes TEXT,threshold INTEGER,allow_buy INTEGER,allow_sell INTEGER,sound INTEGER,vibration INTEGER,updated_ts INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS app_setting(key TEXT PRIMARY KEY,value TEXT,updated_ts INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS price_alarm(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE,symbol TEXT,condition TEXT,target REAL,duration_sec INTEGER,observation TEXT,status TEXT DEFAULT 'ACTIVE',created_ts INTEGER,armed_since INTEGER,triggered_ts INTEGER,updated_ts INTEGER,last_price REAL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS explosion_event(id INTEGER PRIMARY KEY AUTOINCREMENT,uid TEXT UNIQUE,symbol TEXT,timeframe TEXT,detected_ts INTEGER,candle_close INTEGER,price REAL,direction TEXT,status TEXT DEFAULT 'PENDING',expiry_ts INTEGER,resolved_ts INTEGER,target_price REAL,stop_price REAL,atr REAL,volume_ratio REAL,atr_ratio REAL,adx REAL,previous_adx REAL,aligned_timeframes INTEGER,available_timeframes INTEGER,breakout_level REAL,conditions TEXT,mfe REAL DEFAULT 0,mae REAL DEFAULT 0,last_price REAL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS explosion_alert_outbox(explosion_uid TEXT PRIMARY KEY,created_ts INTEGER,status TEXT DEFAULT 'PENDING',attempts INTEGER DEFAULT 0,last_error TEXT DEFAULT '',sent_ts INTEGER)""")
        db.execSQL("""CREATE VIEW IF NOT EXISTS signal_outcome AS SELECT uid AS signal_uid,symbol,timeframe,status,outcome_quality,outcome_reason,outcome_resolved_ts,outcome_window_min,threshold_pct,target_price,stop_price,persistence_status,persistence_quality,legacy_status,mfe,mae,last_price,updated_ts FROM signal""")
        db.execSQL("""CREATE VIEW IF NOT EXISTS historical_pattern AS SELECT ss.*,s.status AS outcome,s.outcome_quality,s.outcome_reason,s.outcome_resolved_ts,s.persistence_status,s.mfe,s.mae,s.last_price,s.updated_ts FROM signal_snapshot ss JOIN signal s ON s.uid=ss.signal_uid WHERE s.status IN ('HIT','FAIL','NEUTRAL') AND s.outcome_quality='EXACT_FIRST_TOUCH_5M_V2'""")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_analysis_symbol_tf_ts ON analysis(symbol,timeframe,ts DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_signal_symbol_tf_ts ON signal(symbol,timeframe,ts DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_follow_due ON followup(status,due_ts)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_persistence_follow_due ON persistence_followup(status,due_ts)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_diag_ts ON diagnostic(ts DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_snapshot_learning ON signal_snapshot(symbol,timeframe,regime,direction,ts DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_candle_cache_close ON candle_cache(symbol,timeframe,close_time DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_outbox_status ON alert_outbox(status,created_ts)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_price_alarm_status ON price_alarm(status,symbol)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_explosion_symbol_tf_ts ON explosion_event(symbol,timeframe,detected_ts DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_explosion_status_expiry ON explosion_event(status,expiry_ts)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_explosion_outbox_status ON explosion_alert_outbox(status,created_ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.beginTransaction()
            try {
                db.execSQL("ALTER TABLE analysis RENAME TO analysis_v1")
                db.execSQL("ALTER TABLE signal RENAME TO signal_v1")
                createSchema(db)
                db.execSQL("""INSERT INTO analysis(symbol,timeframe,ts,candle_close,price,direction,score,confidence,probability,presignal,signal,state,rejection,explanation,rsi,adx,atr,atr_pct,volume_ratio,regime,structure,mtf_score,mtf_summary,risk,forecast_low,forecast_high,invalidation,trend_score,momentum_score,volume_score,structure_score,sr_score,mtf_component,volatility_score,historical_score,btc_score,fundamental_score,comparable_cases,hist_probability,stat_confidence)
                    SELECT symbol,timeframe,ts,ts,price,direction,score,confidence,NULL,presignal,signal,CASE WHEN signal=1 THEN 'SIGNAL' WHEN presignal=1 THEN 'PRESIGNAL' ELSE 'EVALUATION' END,rejection,explanation,rsi,adx,atr,0,volume_ratio,regime,'MIXED',0,'sin datos MTF','MEDIO',0,0,price,0,0,0,0,0,0,0,0,0,0,0,NULL,'INSUFFICIENT' FROM analysis_v1""")
                db.execSQL("""INSERT OR IGNORE INTO signal(uid,symbol,timeframe,ts,candle_close,price,direction,score,confidence,probability,atr,risk,status,mfe,mae,last_price,updated_ts)
                    SELECT uid,symbol,timeframe,ts,ts,price,direction,score,confidence,NULL,0,'MEDIO',status,0,0,price,ts FROM signal_v1""")
                db.execSQL("DROP TABLE analysis_v1")
                db.execSQL("DROP TABLE signal_v1")
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 3) {
            db.beginTransaction()
            try {
                try { db.execSQL("ALTER TABLE analysis ADD COLUMN data_quality TEXT DEFAULT 'COMPLETE'") } catch (_: Throwable) { }
                try { db.execSQL("ALTER TABLE analysis ADD COLUMN missing_data TEXT DEFAULT ''") } catch (_: Throwable) { }
                db.execSQL("""UPDATE analysis SET data_quality='LEGACY_INCOMPLETE',missing_data='Registro anterior sin desglose verificable' WHERE COALESCE(atr_pct,0)<=0 OR COALESCE(mtf_summary,'')='sin datos MTF' OR COALESCE(mtf_summary,'') LIKE '%N/D%'""")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_analysis_symbol_tf_ts ON analysis(symbol,timeframe,ts DESC)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_signal_symbol_tf_ts ON signal(symbol,timeframe,ts DESC)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_follow_due ON followup(status,due_ts)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_diag_ts ON diagnostic(ts DESC)")
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 4) {
            db.beginTransaction()
            try {
                createSchema(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 5) {
            db.beginTransaction()
            try {
                listOf(
                    "change_pct REAL", "plus_di REAL", "minus_di REAL", "ema20 REAL", "ema50 REAL", "ema200 REAL",
                    "ema20_slope REAL", "ema50_slope REAL", "volume_expansion INTEGER DEFAULT 0", "support REAL", "resistance REAL",
                    "support_low REAL", "support_high REAL", "resistance_low REAL", "resistance_high REAL"
                ).forEach { column -> try { db.execSQL("ALTER TABLE analysis ADD COLUMN $column") } catch (_: Throwable) { } }
                createSchema(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 6) {
            db.beginTransaction()
            try {
                listOf(
                    "rsi_slope REAL", "rsi_bull_div INTEGER", "rsi_bear_div INTEGER",
                    "support_touches INTEGER", "resistance_touches INTEGER", "support_strength INTEGER", "resistance_strength INTEGER",
                    "breakout_up INTEGER", "breakout_down INTEGER", "false_breakout INTEGER", "expected_move_atr REAL",
                    "trend_score INTEGER", "momentum_score INTEGER", "volume_score INTEGER", "structure_score INTEGER", "sr_score INTEGER",
                    "mtf_component INTEGER", "volatility_score INTEGER", "historical_score INTEGER",
                    "fundamental_headlines INTEGER", "fundamental_positive INTEGER", "fundamental_negative INTEGER", "fundamental_summary TEXT",
                    "comparable_cases INTEGER", "calibration_wins INTEGER", "calibration_losses INTEGER", "calibration_neutral INTEGER",
                    "hist_probability INTEGER", "stat_confidence TEXT", "decision_threshold INTEGER", "closed_candle INTEGER",
                    "allow_buy INTEGER", "allow_sell INTEGER", "missing_data TEXT DEFAULT ''"
                ).forEach { column -> try { db.execSQL("ALTER TABLE signal_snapshot ADD COLUMN $column") } catch (_: Throwable) { } }
                // Los snapshots de V4/V5 se conservan para auditoría, pero no se
                // presentan como V6 exactos ni alimentan comparables incompletos.
                db.execSQL("UPDATE signal_snapshot SET snapshot_quality='LEGACY_V4' WHERE rsi_slope IS NULL OR decision_threshold IS NULL")
                createSchema(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 7) {
            db.beginTransaction()
            try {
                try { db.execSQL("ALTER TABLE followup ADD COLUMN market_ts INTEGER") } catch (_: Throwable) { }
                try { db.execSQL("ALTER TABLE signal ADD COLUMN outcome_quality TEXT DEFAULT 'EXACT_5M'") } catch (_: Throwable) { }
                db.execSQL("UPDATE signal SET outcome_quality='LEGACY_SAMPLED'")
                db.execSQL("DROP VIEW IF EXISTS signal_outcome")
                db.execSQL("DROP VIEW IF EXISTS historical_pattern")
                createSchema(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 8) {
            db.beginTransaction()
            try {
                // V3.5 partía de la hora de ejecución del ciclo, no siempre del
                // cierre que originó la señal. Se conserva para auditoría, pero
                // no alimentará aprendizaje ni estadísticas exactas V3.6.
                db.execSQL("UPDATE signal SET outcome_quality='LEGACY_MISALIGNED_5M' WHERE outcome_quality='EXACT_5M'")
                createSchema(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 9) {
            db.beginTransaction()
            try {
                // Módulo V3.7 aislado: sólo añade tablas; no reescribe señales,
                // análisis, seguimientos, alarmas ni estadísticas existentes.
                createSchema(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
        if (oldVersion < 10) {
            db.beginTransaction()
            try {
                listOf(
                    "outcome_reason TEXT DEFAULT ''", "outcome_resolved_ts INTEGER", "outcome_window_min INTEGER DEFAULT 1440",
                    "threshold_pct REAL DEFAULT 0", "target_price REAL", "stop_price REAL",
                    "persistence_status TEXT DEFAULT 'PENDING'", "persistence_quality TEXT DEFAULT 'ADAPTIVE_V2'", "legacy_status TEXT"
                ).forEach { column -> try { db.execSQL("ALTER TABLE signal ADD COLUMN $column") } catch (_: Throwable) { } }
                db.execSQL("""CREATE TABLE IF NOT EXISTS persistence_followup(id INTEGER PRIMARY KEY AUTOINCREMENT,signal_uid TEXT,horizon_min INTEGER,due_ts INTEGER,checked_ts INTEGER,market_ts INTEGER,price REAL,move_pct REAL,status TEXT DEFAULT 'PENDING',profile TEXT DEFAULT 'ADAPTIVE_V2',UNIQUE(signal_uid,horizon_min))""")
                db.execSQL("UPDATE signal SET legacy_status=status,persistence_status=status,persistence_quality='LEGACY_FIXED_V1' WHERE atr>0")
                db.execSQL("""UPDATE signal SET outcome_window_min=CASE timeframe WHEN '4h' THEN 4320 WHEN '1d' THEN 20160 ELSE 1440 END,
                    threshold_pct=MAX(0.45,CASE WHEN price>0 THEN (atr/price*100.0)*0.55 ELSE 0.45 END) WHERE atr>0""")
                db.execSQL("""UPDATE signal SET target_price=CASE WHEN direction LIKE '%SELL%' THEN price*(1.0-threshold_pct/100.0) ELSE price*(1.0+threshold_pct/100.0) END,
                    stop_price=CASE WHEN direction LIKE '%SELL%' THEN price*(1.0+threshold_pct/100.0) ELSE price*(1.0-threshold_pct/100.0) END WHERE atr>0""")
                // Las señales V3.6/V3.7 se reclasificarán desde Binance por
                // primer toque. Su resultado anterior queda en legacy_status.
                db.execSQL("""UPDATE signal SET status='PENDING',outcome_quality='RECLASSIFY_FIRST_TOUCH_V2',outcome_reason='',outcome_resolved_ts=NULL,
                    persistence_status='PENDING',persistence_quality='ADAPTIVE_V2' WHERE atr>0 AND outcome_quality='EXACT_POST_SIGNAL_5M'""")
                insertAdaptivePersistenceRows(db, "outcome_quality IN ('RECLASSIFY_FIRST_TOUCH_V2','EXACT_FIRST_TOUCH_5M_V2')")
                db.execSQL("DROP VIEW IF EXISTS signal_outcome")
                db.execSQL("DROP VIEW IF EXISTS historical_pattern")
                createSchema(db)
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        }
    }

    private fun insertAdaptivePersistenceRows(db: SQLiteDatabase, signalWhere: String) {
        val horizons = mapOf(
            "15m" to listOf(15, 60, 240, 1440), "30m" to listOf(30, 120, 480, 1440),
            "1h" to listOf(60, 240, 720, 1440), "4h" to listOf(240, 720, 1440, 4320),
            "1d" to listOf(1440, 4320, 10080, 20160)
        )
        horizons.forEach { (timeframe, minutes) ->
            minutes.forEach { horizon ->
                db.execSQL("""INSERT OR IGNORE INTO persistence_followup(signal_uid,horizon_min,due_ts,status,profile)
                    SELECT uid,$horizon,candle_close+${horizon}*60000,'PENDING','ADAPTIVE_V2' FROM signal
                    WHERE timeframe=? AND atr>0 AND $signalWhere""", arrayOf(timeframe))
            }
        }
    }

    @Synchronized fun saveAnalysis(a: AnalysisResult, deduplicateClosedCandle: Boolean): Boolean {
        if (deduplicateClosedCandle) {
            val existingQuality = readableDatabase.rawQuery(
                "SELECT data_quality FROM analysis WHERE symbol=? AND timeframe=? AND candle_close=? ORDER BY id DESC LIMIT 1",
                arrayOf(a.symbol, a.timeframe, a.candleCloseTime.toString())
            ).use { if (it.moveToFirst()) it.getString(0) else null }
            if (existingQuality == "COMPLETE" || (existingQuality != null && !a.dataComplete)) return false
        }
        val v = ContentValues().apply {
            put("symbol", a.symbol); put("timeframe", a.timeframe); put("ts", a.timestamp); put("candle_close", a.candleCloseTime); put("price", a.indicators.price); put("change_pct", priceChangePct(a.symbol, a.timeframe, a.indicators.price, a.candleCloseTime))
            put("direction", a.direction.name); put("score", a.score); put("confidence", a.confidence); if (a.probability == null) putNull("probability") else put("probability", a.probability)
            put("presignal", if (a.isPreSignal) 1 else 0); put("signal", if (a.isSignal) 1 else 0); put("state", a.state.name); put("rejection", a.rejectionReason); put("explanation", JSONArray(a.explanation).toString())
            put("rsi", a.indicators.rsi14); put("adx", a.indicators.adx14); put("plus_di", a.indicators.plusDi); put("minus_di", a.indicators.minusDi)
            put("atr", a.indicators.atr14); put("atr_pct", a.indicators.atrPct); put("ema20", a.indicators.ema20); put("ema50", a.indicators.ema50); put("ema200", a.indicators.ema200)
            put("ema20_slope", a.indicators.ema20SlopePct); put("ema50_slope", a.indicators.ema50SlopePct); put("volume_ratio", a.indicators.volumeRatio); put("volume_expansion", if (a.indicators.volumeExpansion) 1 else 0)
            put("support", a.indicators.support); put("resistance", a.indicators.resistance); put("support_low", a.indicators.supportZone.low); put("support_high", a.indicators.supportZone.high)
            put("resistance_low", a.indicators.resistanceZone.low); put("resistance_high", a.indicators.resistanceZone.high); put("regime", a.indicators.regime.name); put("structure", a.indicators.structure.name)
            put("mtf_score", a.mtf.alignmentScore); put("mtf_summary", a.mtf.summary); put("risk", a.risk); put("forecast_low", a.forecastLowPct); put("forecast_high", a.forecastHighPct); put("invalidation", a.invalidationPrice)
            put("trend_score", a.breakdown.trend); put("momentum_score", a.breakdown.momentum); put("volume_score", a.breakdown.volume); put("structure_score", a.breakdown.structure); put("sr_score", a.breakdown.supportResistance); put("mtf_component", a.breakdown.mtf); put("volatility_score", a.breakdown.volatility); put("historical_score", a.breakdown.historical); put("btc_score", a.breakdown.btcContext); put("fundamental_score", a.breakdown.fundamental)
            put("comparable_cases", a.calibration.comparableCases); if (a.calibration.estimatedProbability == null) putNull("hist_probability") else put("hist_probability", a.calibration.estimatedProbability); put("stat_confidence", a.calibration.confidence.name)
            put("data_quality", if (a.dataComplete) "COMPLETE" else "INCOMPLETE"); put("missing_data", a.missingData.joinToString(", "))
        }
        writableDatabase.insertOrThrow("analysis", null, v)
        writableDatabase.execSQL("DELETE FROM analysis WHERE id NOT IN (SELECT id FROM analysis ORDER BY id DESC LIMIT 50000)")
        return true
    }

    private fun priceChangePct(symbol: String, timeframe: String, current: Double, candleClose: Long): Double {
        val previous = readableDatabase.rawQuery(
            "SELECT close FROM candle_cache WHERE symbol=? AND timeframe=? AND close_time<? ORDER BY close_time DESC LIMIT 1",
            arrayOf(symbol, timeframe, candleClose.toString())
        ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
        return if (previous > 0.0) (current - previous) / previous * 100.0 else 0.0
    }

    data class SignalInsertResult(val inserted: Boolean, val uid: String)

    data class PendingAlert(
        val uid: String,
        val symbol: String,
        val timeframe: String,
        val eventAt: Long,
        val direction: String,
        val confidence: Int,
        val probability: Int?,
        val price: Double,
        val risk: String,
        val comparableCases: Int
    )

    @Synchronized fun insertSignalAndAlertIfNew(a: AnalysisResult, cooldownMinutes: Int): SignalInsertResult {
        val side = if (a.direction == Direction.BUY || a.direction == Direction.REVERSAL_BUY) "BUY" else "SELL"
        val uid = "${a.symbol}-${a.timeframe}-${a.candleCloseTime}-$side"
        val db = writableDatabase
        db.beginTransaction()
        try {
            val latest = db.rawQuery("SELECT ts,confidence FROM signal WHERE symbol=? AND timeframe=? AND direction LIKE ? ORDER BY ts DESC LIMIT 1", arrayOf(a.symbol, a.timeframe, "%$side%"))
            latest.use { c ->
                if (c.moveToFirst()) {
                    val age = a.timestamp - c.getLong(0)
                    if (age < cooldownMinutes * 60_000L && a.confidence <= c.getInt(1) + 5) return SignalInsertResult(false, uid)
                }
            }
            val v = ContentValues().apply {
                val thresholdPct = OutcomeTracker.movementThresholdPct(a.indicators.price, a.indicators.atr14)
                put("uid", uid); put("symbol", a.symbol); put("timeframe", a.timeframe); put("ts", a.timestamp); put("candle_close", a.candleCloseTime); put("price", a.indicators.price); put("direction", a.direction.name); put("score", a.score); put("confidence", a.confidence); if (a.probability == null) putNull("probability") else put("probability", a.probability); put("atr", a.indicators.atr14); put("risk", a.risk)
                put("status", OutcomeStatus.PENDING.name); put("outcome_quality", "EXACT_FIRST_TOUCH_5M_V2"); put("outcome_reason", ""); put("outcome_window_min", OutcomeTracker.outcomeWindowMinutes(a.timeframe)); put("threshold_pct", thresholdPct)
                put("target_price", OutcomeTracker.targetPrice(a.indicators.price, a.direction.name, thresholdPct)); put("stop_price", OutcomeTracker.stopPrice(a.indicators.price, a.direction.name, thresholdPct))
                put("persistence_status", OutcomeStatus.PENDING.name); put("persistence_quality", "ADAPTIVE_V2"); putNull("legacy_status")
                put("last_price", a.indicators.price); put("updated_ts", a.timestamp)
            }
            val id = db.insertWithOnConflict("signal", null, v, SQLiteDatabase.CONFLICT_IGNORE)
            if (id == -1L) return SignalInsertResult(false, uid)
            db.insertOrThrow("signal_snapshot", null, signalSnapshotValues(uid, a))
            OutcomeTracker.persistenceHorizonsMinutes(a.timeframe).forEach { horizon ->
                ContentValues().also { fv ->
                    fv.put("signal_uid", uid); fv.put("horizon_min", horizon); fv.put("due_ts", a.candleCloseTime + horizon * 60_000L)
                    fv.put("status", OutcomeStatus.PENDING.name); fv.put("profile", "ADAPTIVE_V2")
                    db.insertWithOnConflict("persistence_followup", null, fv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            ContentValues().also { av ->
                av.put("signal_uid", uid); av.put("ts", a.timestamp); av.put("symbol", a.symbol); av.put("kind", "SIGNAL"); av.put("message", "${a.direction} ${a.confidence}%")
                db.insertWithOnConflict("alert_event", null, av, SQLiteDatabase.CONFLICT_IGNORE)
            }
            ContentValues().also { outbox ->
                outbox.put("signal_uid", uid); outbox.put("created_ts", a.timestamp); outbox.put("status", "PENDING")
                db.insertOrThrow("alert_outbox", null, outbox)
            }
            db.setTransactionSuccessful()
            return SignalInsertResult(true, uid)
        } finally { db.endTransaction() }
    }

    private fun signalSnapshotValues(uid: String, a: AnalysisResult) = ContentValues().apply {
        val i = a.indicators
        put("signal_uid", uid); put("symbol", a.symbol); put("timeframe", a.timeframe); put("ts", a.timestamp); put("candle_close", a.candleCloseTime)
        put("price", i.price); put("direction", a.direction.name); put("score", a.score); put("confidence", a.confidence)
        if (a.probability == null) putNull("probability") else put("probability", a.probability)
        put("risk", a.risk); put("rsi", i.rsi14); put("rsi_slope", i.rsiSlope); put("rsi_bull_div", if (i.rsiBullishDivergence) 1 else 0); put("rsi_bear_div", if (i.rsiBearishDivergence) 1 else 0)
        put("adx", i.adx14); put("plus_di", i.plusDi); put("minus_di", i.minusDi)
        put("atr", i.atr14); put("atr_pct", i.atrPct); put("ema20", i.ema20); put("ema50", i.ema50); put("ema200", i.ema200)
        put("ema20_slope", i.ema20SlopePct); put("ema50_slope", i.ema50SlopePct); put("volume_ratio", i.volumeRatio); put("volume_expansion", if (i.volumeExpansion) 1 else 0)
        put("support", i.support); put("resistance", i.resistance); put("support_low", i.supportZone.low); put("support_high", i.supportZone.high)
        put("resistance_low", i.resistanceZone.low); put("resistance_high", i.resistanceZone.high)
        put("support_touches", i.supportZone.touches); put("resistance_touches", i.resistanceZone.touches); put("support_strength", i.supportZone.strength); put("resistance_strength", i.resistanceZone.strength)
        put("structure", i.structure.name); put("regime", i.regime.name); put("breakout_up", if (i.breakoutUp) 1 else 0); put("breakout_down", if (i.breakoutDown) 1 else 0); put("false_breakout", if (i.falseBreakout) 1 else 0); put("expected_move_atr", i.expectedMoveAtr)
        put("mtf_score", a.mtf.alignmentScore); put("mtf_summary", a.mtf.summary); put("btc_regime", a.btcRegime?.name ?: "UNKNOWN")
        put("trend_score", a.breakdown.trend); put("momentum_score", a.breakdown.momentum); put("volume_score", a.breakdown.volume); put("structure_score", a.breakdown.structure); put("sr_score", a.breakdown.supportResistance); put("mtf_component", a.breakdown.mtf); put("volatility_score", a.breakdown.volatility); put("historical_score", a.breakdown.historical); put("btc_score", a.breakdown.btcContext); put("fundamental_score", a.breakdown.fundamental)
        put("fundamental_headlines", a.fundamental.headlineCount); put("fundamental_positive", a.fundamental.positiveCount); put("fundamental_negative", a.fundamental.negativeCount); put("fundamental_summary", a.fundamental.summary)
        put("comparable_cases", a.calibration.comparableCases); put("calibration_wins", a.calibration.wins); put("calibration_losses", a.calibration.losses); put("calibration_neutral", a.calibration.neutral); if (a.calibration.estimatedProbability == null) putNull("hist_probability") else put("hist_probability", a.calibration.estimatedProbability); put("stat_confidence", a.calibration.confidence.name)
        put("forecast_low", a.forecastLowPct); put("forecast_high", a.forecastHighPct); put("invalidation", a.invalidationPrice)
        put("decision_threshold", a.decisionThreshold); put("closed_candle", if (a.closedCandleEvaluation) 1 else 0); put("allow_buy", if (a.allowBuy) 1 else 0); put("allow_sell", if (a.allowSell) 1 else 0)
        put("explanation", JSONArray(a.explanation).toString()); put("missing_data", a.missingData.joinToString(", ")); put("snapshot_quality", if (a.dataComplete) "COMPLETE" else "INCOMPLETE")
    }

    fun pendingAlerts(limit: Int = 20): List<PendingAlert> {
        val out = mutableListOf<PendingAlert>()
        readableDatabase.rawQuery(
            """SELECT o.signal_uid,ss.symbol,ss.timeframe,ss.candle_close,ss.direction,ss.confidence,ss.probability,ss.price,ss.risk,
                COALESCE((SELECT comparable_cases FROM analysis a WHERE a.symbol=ss.symbol AND a.timeframe=ss.timeframe AND ABS(a.ts-ss.ts)<120000 ORDER BY a.ts DESC LIMIT 1),0)
                FROM alert_outbox o JOIN signal_snapshot ss ON ss.signal_uid=o.signal_uid
                WHERE o.status IN ('PENDING','FAILED') AND o.attempts<5 ORDER BY o.created_ts LIMIT ?""",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) out += PendingAlert(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getInt(5), if (c.isNull(6)) null else c.getInt(6), c.getDouble(7), c.getString(8), c.getInt(9))
        }
        return out
    }

    fun markAlertSent(uid: String) {
        ContentValues().also { v -> v.put("status", "SENT"); v.put("sent_ts", System.currentTimeMillis()); v.put("last_error", ""); writableDatabase.update("alert_outbox", v, "signal_uid=?", arrayOf(uid)) }
    }

    fun markAlertFailed(uid: String, error: String) {
        writableDatabase.execSQL("UPDATE alert_outbox SET status='FAILED',attempts=attempts+1,last_error=? WHERE signal_uid=?", arrayOf(error.take(300), uid))
    }

    fun calibration(symbol: String, timeframe: String, i: IndicatorSet, direction: Direction, mtfScore: Int, btcRegime: Regime?): HistoricalCalibration {
        val supportDistance = if (i.price > 0) (i.price - i.support) / i.price * 100.0 else 0.0
        val resistanceDistance = if (i.price > 0) (i.resistance - i.price) / i.price * 100.0 else 0.0
        val sql = """SELECT s.uid,s.status FROM signal s
            JOIN signal_snapshot ss ON ss.signal_uid=s.uid
            WHERE s.symbol=? AND s.timeframe=? AND ss.snapshot_quality='COMPLETE' AND s.outcome_quality='EXACT_FIRST_TOUCH_5M_V2'
            AND ss.regime=? AND ss.structure=? AND s.direction LIKE ?
            AND ABS(ss.rsi-?)<=10 AND ABS(ss.adx-?)<=12
            AND ss.atr_pct BETWEEN ? AND ? AND ss.volume_ratio BETWEEN ? AND ?
            AND (ss.ema20-ss.ema50) * (? - ?) >= 0
            AND ABS(ss.mtf_score-?)<=8 AND ss.mtf_score * ? >= 0
            AND ABS(((ss.price-ss.support)/ss.price*100)-?)<=3
            AND ABS(((ss.resistance-ss.price)/ss.price*100)-?)<=3
            AND (?='UNKNOWN' OR ss.btc_regime=?)
            AND s.status IN ('HIT','FAIL','NEUTRAL')
            ORDER BY s.ts DESC LIMIT 500"""
        val side = if (direction == Direction.SELL || direction == Direction.REVERSAL_SELL) "%SELL%" else "%BUY%"
        var wins = 0; var losses = 0; var neutral = 0
        val args = arrayOf(
            symbol, timeframe, i.regime.name, i.structure.name, side,
            i.rsi14.toString(), i.adx14.toString(), (i.atrPct * 0.55).toString(), (i.atrPct * 1.8).toString(),
            (i.volumeRatio - 0.6).coerceAtLeast(0.0).toString(), (i.volumeRatio + 0.6).toString(),
            i.ema20.toString(), i.ema50.toString(), "0", "0", supportDistance.toString(), resistanceDistance.toString(), "UNKNOWN", "UNKNOWN"
        )
        // Los valores de contexto se sustituyen a continuación para mantener
        // el orden de argumentos legible y evitar snapshots ambiguos.
        args[13] = mtfScore.toString()
        args[14] = mtfScore.toString()
        val btcName = btcRegime?.name ?: "UNKNOWN"
        args[17] = btcName
        args[18] = btcName
        readableDatabase.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) when (c.getString(1)) {
                OutcomeStatus.HIT.name -> wins++
                OutcomeStatus.FAIL.name -> losses++
                OutcomeStatus.NEUTRAL.name -> neutral++
            }
        }
        val n = wins + losses + neutral
        val resolved = wins + losses
        // No se publica una probabilidad con una muestra pequeña. Se exigen al
        // menos 10 casos comparables y 10 resultados BUY/SELL ya resueltos.
        val p = if (resolved >= MIN_CALIBRATION_CASES) (100.0 * wins / resolved).toInt() else null
        val conf = when {
            resolved >= 100 -> StatisticalConfidence.HIGH
            resolved >= 30 -> StatisticalConfidence.MEDIUM
            resolved >= MIN_CALIBRATION_CASES -> StatisticalConfidence.LOW
            else -> StatisticalConfidence.INSUFFICIENT
        }
        return HistoricalCalibration(n, wins, losses, neutral, p, conf)
    }


    fun pendingFollowupStarts(): Map<String, Long> {
        val out = linkedMapOf<String, Long>()
        readableDatabase.rawQuery(
            """SELECT s.symbol,MIN(s.candle_close + 1) FROM signal s
                WHERE s.atr>0 AND s.outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2')
                AND (s.status='PENDING' OR EXISTS(SELECT 1 FROM persistence_followup pf WHERE pf.signal_uid=s.uid AND pf.status='PENDING'))
                GROUP BY s.symbol""", null
        ).use { c -> while (c.moveToNext()) out[c.getString(0)] = c.getLong(1) }
        return out
    }

    @Synchronized fun updateFollowups(currentPrices: Map<String, Double>, histories: Map<String, List<Candle>>, now: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        db.rawQuery("""SELECT s.uid,s.symbol,s.candle_close,s.price,s.direction,s.atr,s.mfe,s.mae,s.status,
            s.candle_close+s.outcome_window_min*60000,s.threshold_pct,s.outcome_quality,s.persistence_status,
            COALESCE((SELECT MAX(pf.due_ts) FROM persistence_followup pf WHERE pf.signal_uid=s.uid),s.candle_close)
            FROM signal s WHERE s.atr>0 AND s.outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2')
            AND (s.status='PENDING' OR EXISTS(SELECT 1 FROM persistence_followup pf WHERE pf.signal_uid=s.uid AND pf.status='PENDING'))""", null).use { c ->
            while (c.moveToNext()) {
                val uid = c.getString(0); val symbol = c.getString(1); val signalClose = c.getLong(2); val entry = c.getDouble(3); val dir = c.getString(4); val atr = c.getDouble(5)
                val operationStatus = c.getString(8); val outcomeDue = c.getLong(9)
                val thresholdPct = c.getDouble(10).takeIf { it > 0.0 } ?: OutcomeTracker.movementThresholdPct(entry, atr)
                val persistenceDue = c.getLong(13)
                val requiredEnd = maxOf(outcomeDue, persistenceDue)
                // Una vela se admite sólo si comenzó después del instante de
                // señal. Así sus máximos/mínimos nunca contienen precio previo.
                val signalCandles = OutcomeTracker.candlesStrictlyAfter(signalClose, minOf(now, requiredEnd), histories[symbol].orEmpty())
                val excursions = OutcomeTracker.excursions(entry, dir, signalCandles)
                val newMfe = maxOf(c.getDouble(6), excursions.mfe)
                val newMae = minOf(c.getDouble(7), excursions.mae)
                ContentValues().also { v ->
                    currentPrices[symbol]?.let { v.put("last_price", it) }
                    v.put("mfe", newMfe); v.put("mae", newMae); v.put("updated_ts", now)
                    db.update("signal", v, "uid=?", arrayOf(uid))
                }

                if (operationStatus == OutcomeStatus.PENDING.name) {
                    val operationCandles = signalCandles.filter { it.closeTime <= outcomeDue }
                    val resolution = OutcomeTracker.firstTouchResolution(entry, Direction.valueOf(dir), operationCandles, thresholdPct)
                    val deadlineCovered = operationCandles.lastOrNull()?.let { outcomeDue - it.closeTime in 0..10 * 60_000L } == true
                    val finalStatus = resolution?.status ?: if (now >= outcomeDue && deadlineCovered) OutcomeStatus.NEUTRAL else null
                    if (finalStatus != null) {
                        val reason = resolution?.reason ?: "NO_TOUCH_BY_DEADLINE"
                        val resolvedAt = resolution?.marketTimestamp ?: outcomeDue
                        ContentValues().also { v ->
                            v.put("status", finalStatus.name); v.put("outcome_quality", "EXACT_FIRST_TOUCH_5M_V2")
                            v.put("outcome_reason", reason); v.put("outcome_resolved_ts", resolvedAt); v.put("updated_ts", now)
                            db.update("signal", v, "uid=?", arrayOf(uid))
                        }
                        log(symbol, "OPERATION_RESULT", "$uid => ${finalStatus.name} • $reason • primer toque 5m • MFE ${"%.2f".format(newMfe)}% • MAE ${"%.2f".format(newMae)}%")
                    }
                }

                db.rawQuery("SELECT id,horizon_min,due_ts FROM persistence_followup WHERE signal_uid=? AND status='PENDING' AND due_ts<=? ORDER BY horizon_min", arrayOf(uid, now.toString())).use { f ->
                    while (f.moveToNext()) {
                        val horizon = f.getInt(1)
                        val observation = OutcomeTracker.observationAt(entry, dir, f.getLong(2), signalCandles, thresholdPct) ?: continue
                        ContentValues().also { v ->
                            v.put("checked_ts", now); v.put("market_ts", observation.marketTimestamp); v.put("price", observation.price)
                            v.put("move_pct", observation.movePct); v.put("status", observation.status.name)
                            db.update("persistence_followup", v, "id=?", arrayOf(f.getLong(0).toString()))
                        }
                        log(symbol, "PERSISTENCE", "$uid • ${horizon}m • ${observation.status.name} • ${"%.2f".format(observation.movePct)}% • vela ${observation.marketTimestamp}")
                    }
                }
                val allDone = db.rawQuery("SELECT COUNT(*) FROM persistence_followup WHERE signal_uid=? AND status='PENDING'", arrayOf(uid)).use { x -> x.moveToFirst(); x.getInt(0) == 0 }
                if (allDone) {
                    val statuses = mutableListOf<String>()
                    db.rawQuery("SELECT status FROM persistence_followup WHERE signal_uid=?", arrayOf(uid)).use { x -> while (x.moveToNext()) statuses += x.getString(0) }
                    val final = when {
                        statuses.count { it == OutcomeStatus.HIT.name } >= 2 -> OutcomeStatus.HIT.name
                        statuses.count { it == OutcomeStatus.FAIL.name } >= 2 -> OutcomeStatus.FAIL.name
                        else -> OutcomeStatus.NEUTRAL.name
                    }
                    ContentValues().also { v -> v.put("persistence_status", final); v.put("persistence_quality", "ADAPTIVE_V2"); v.put("updated_ts", now); db.update("signal", v, "uid=?", arrayOf(uid)) }
                    log(symbol, "PERSISTENCE_RESULT", "$uid => $final • horizontes adaptados")
                }
            }
        }
    }

    @Synchronized fun syncConfiguration(context: Context) {
        val now = System.currentTimeMillis()
        val selected = Prefs.watchlist(context)
        val db = writableDatabase
        db.beginTransaction()
        try {
            val catalog = (Assets.supported + (Prefs.catalogSymbols(context) + selected + Prefs.favorites(context)).map { CryptoAsset(it, it) }).associateBy { it.symbol }.values
            catalog.forEach { asset ->
                ContentValues().also { v ->
                    v.put("symbol", asset.symbol); v.put("name", asset.name); v.put("enabled", if (asset.symbol in selected) 1 else 0); v.put("updated_ts", now)
                    db.insertWithOnConflict("crypto_asset", null, v, SQLiteDatabase.CONFLICT_REPLACE)
                }
                ContentValues().also { v ->
                    v.put("symbol", asset.symbol); v.put("timeframes", Prefs.symbolTimeframes(context, asset.symbol).sorted().joinToString(",")); v.put("threshold", Prefs.symbolThreshold(context, asset.symbol))
                    v.put("allow_buy", if (Prefs.symbolAllowBuy(context, asset.symbol)) 1 else 0); v.put("allow_sell", if (Prefs.symbolAllowSell(context, asset.symbol)) 1 else 0)
                    v.put("sound", if (Prefs.symbolSoundEnabled(context, asset.symbol)) 1 else 0); v.put("vibration", if (Prefs.symbolVibrationEnabled(context, asset.symbol)) 1 else 0); v.put("updated_ts", now)
                    db.insertWithOnConflict("watch_config", null, v, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            mapOf(
                "watching" to Prefs.watching(context).toString(), "timeframe" to Prefs.timeframe(context), "threshold" to Prefs.threshold(context).toString(),
                "interval_minutes" to Prefs.intervalMinutes(context).toString(), "cooldown_minutes" to Prefs.cooldownMinutes(context).toString(),
                "closed_candle" to Prefs.closedCandleMode(context).toString(), "news_enabled" to Prefs.newsEnabled(context).toString(),
                "sound_enabled" to Prefs.soundEnabled(context).toString(), "vibration_enabled" to Prefs.vibrationEnabled(context).toString(),
                "explosion_detector_enabled" to Prefs.explosionDetectorEnabled(context).toString(),
                "explosion_sound_enabled" to Prefs.explosionSoundEnabled(context).toString(),
                "explosion_vibration_enabled" to Prefs.explosionVibrationEnabled(context).toString(),
                "menu_on_left" to Prefs.menuOnLeft(context).toString(), "dashboard_sort" to Prefs.dashboardSort(context),
                "watchlist" to selected.sorted().joinToString(","), "favorites" to Prefs.favorites(context).sorted().joinToString(","),
                "remote_endpoint" to Prefs.remoteEndpoint(context), "remote_configured" to Prefs.remoteEndpoint(context).isNotBlank().toString()
            ).forEach { (key, value) ->
                ContentValues().also { v -> v.put("key", key); v.put("value", value); v.put("updated_ts", now); db.insertWithOnConflict("app_setting", null, v, SQLiteDatabase.CONFLICT_REPLACE) }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun cachedCandles(symbol: String, timeframe: String, limit: Int): List<Candle> {
        val out = mutableListOf<Candle>()
        readableDatabase.rawQuery(
            "SELECT open_time,open,high,low,close,volume,close_time FROM candle_cache WHERE symbol=? AND timeframe=? ORDER BY open_time DESC LIMIT ?",
            arrayOf(symbol, timeframe, limit.toString())
        ).use { c ->
            while (c.moveToNext()) out += Candle(c.getLong(0), c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getDouble(5), c.getLong(6))
        }
        return out.asReversed()
    }

    fun cachedCandlesBetween(symbol: String, timeframe: String, startTime: Long, endTime: Long): List<Candle> {
        val out = mutableListOf<Candle>()
        readableDatabase.rawQuery(
            """SELECT open_time,open,high,low,close,volume,close_time FROM candle_cache
                WHERE symbol=? AND timeframe=? AND close_time>=? AND close_time<=? ORDER BY open_time""",
            arrayOf(symbol, timeframe, startTime.toString(), endTime.toString())
        ).use { c ->
            while (c.moveToNext()) out += Candle(c.getLong(0), c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getDouble(4), c.getDouble(5), c.getLong(6))
        }
        return out
    }

    @Synchronized fun upsertCandles(symbol: String, timeframe: String, candles: List<Candle>) {
        if (candles.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            candles.forEach { candle ->
                ContentValues().also { v ->
                    v.put("symbol", symbol); v.put("timeframe", timeframe); v.put("open_time", candle.openTime); v.put("open", candle.open); v.put("high", candle.high)
                    v.put("low", candle.low); v.put("close", candle.close); v.put("volume", candle.volume); v.put("close_time", candle.closeTime)
                    db.insertWithOnConflict("candle_cache", null, v, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
            val retained = if (timeframe == "5m") 5000 else 1200
            db.execSQL("DELETE FROM candle_cache WHERE symbol=? AND timeframe=? AND open_time NOT IN (SELECT open_time FROM candle_cache WHERE symbol=? AND timeframe=? ORDER BY open_time DESC LIMIT $retained)", arrayOf(symbol, timeframe, symbol, timeframe))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun indicatorPayload(symbol: String, timeframe: String, candleClose: Long): String? = readableDatabase.rawQuery(
        "SELECT payload FROM indicator_cache WHERE symbol=? AND timeframe=? AND candle_close=?",
        arrayOf(symbol, timeframe, candleClose.toString())
    ).use { c -> if (c.moveToFirst()) c.getString(0) else null }

    @Synchronized fun saveIndicatorPayload(symbol: String, timeframe: String, candleClose: Long, payload: String) {
        ContentValues().also { v ->
            v.put("symbol", symbol); v.put("timeframe", timeframe); v.put("candle_close", candleClose); v.put("payload", payload); v.put("updated_ts", System.currentTimeMillis())
            writableDatabase.insertWithOnConflict("indicator_cache", null, v, SQLiteDatabase.CONFLICT_REPLACE)
        }
        writableDatabase.execSQL("DELETE FROM indicator_cache WHERE updated_ts < ?", arrayOf(System.currentTimeMillis() - 14L * 24 * 60 * 60_000))
    }

    data class PriceAlarm(
        val id: Long,
        val uid: String,
        val symbol: String,
        val condition: String,
        val target: Double,
        val durationSeconds: Long,
        val observation: String,
        val status: String,
        val createdAt: Long,
        val armedSince: Long?,
        val triggeredAt: Long?,
        val lastPrice: Double?
    )

    fun createPriceAlarm(symbol: String, condition: String, target: Double, durationSeconds: Long, observation: String): Long {
        require(symbol.isNotBlank() && target > 0.0 && durationSeconds >= 0L)
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("uid", "ALARM-${UUID.randomUUID()}"); put("symbol", symbol.uppercase()); put("condition", condition)
            put("target", target); put("duration_sec", durationSeconds); put("observation", observation.trim()); put("status", "ACTIVE")
            put("created_ts", now); put("updated_ts", now)
        }
        return writableDatabase.insertOrThrow("price_alarm", null, values)
    }

    fun priceAlarms(): List<PriceAlarm> {
        val out = mutableListOf<PriceAlarm>()
        readableDatabase.rawQuery("SELECT id,uid,symbol,condition,target,duration_sec,observation,status,created_ts,armed_since,triggered_ts,last_price FROM price_alarm ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END,created_ts DESC", null).use { c ->
            while (c.moveToNext()) out += PriceAlarm(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getDouble(4), c.getLong(5), c.getString(6) ?: "", c.getString(7), c.getLong(8), if (c.isNull(9)) null else c.getLong(9), if (c.isNull(10)) null else c.getLong(10), if (c.isNull(11)) null else c.getDouble(11))
        }
        return out
    }

    fun setPriceAlarmStatus(id: Long, status: String) {
        require(status in setOf("ACTIVE", "TRIGGERED", "CANCELLED", "EXPIRED"))
        val values = ContentValues().apply { put("status", status); put("updated_ts", System.currentTimeMillis()); if (status == "ACTIVE") { putNull("armed_since"); putNull("triggered_ts") } }
        writableDatabase.update("price_alarm", values, "id=?", arrayOf(id.toString()))
    }

    @Synchronized fun evaluatePriceAlarms(prices: Map<String, Double>, now: Long = System.currentTimeMillis()): List<PriceAlarm> {
        val active = priceAlarms().filter { it.status == "ACTIVE" }
        val triggered = mutableListOf<PriceAlarm>()
        val db = writableDatabase
        db.beginTransaction()
        try {
            active.forEach { alarm ->
                val price = prices[alarm.symbol] ?: return@forEach
                val crossed = if (alarm.condition == "ABOVE") price >= alarm.target else price <= alarm.target
                val armed = when { crossed && alarm.armedSince == null -> now; crossed -> alarm.armedSince; else -> null }
                val fires = crossed && armed != null && now - armed >= alarm.durationSeconds * 1000L
                ContentValues().also { v ->
                    v.put("last_price", price); v.put("updated_ts", now)
                    if (armed == null) v.putNull("armed_since") else v.put("armed_since", armed)
                    if (fires) { v.put("status", "TRIGGERED"); v.put("triggered_ts", now) }
                    db.update("price_alarm", v, "id=?", arrayOf(alarm.id.toString()))
                }
                if (fires) triggered += alarm.copy(status = "TRIGGERED", armedSince = armed, triggeredAt = now, lastPrice = price)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return triggered
    }

    fun activePriceAlarmCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM price_alarm WHERE status='ACTIVE'", null).use { c -> c.moveToFirst(); c.getInt(0) }

    data class ExplosionInsertResult(val inserted: Boolean, val uid: String)

    data class ExplosionEvent(
        val uid: String,
        val symbol: String,
        val timeframe: String,
        val detectedAt: Long,
        val candleClose: Long,
        val price: Double,
        val direction: String,
        val status: String,
        val expiryAt: Long,
        val resolvedAt: Long?,
        val targetPrice: Double,
        val stopPrice: Double,
        val atr: Double,
        val volumeRatio: Double,
        val atrRatio: Double,
        val adx: Double,
        val previousAdx: Double,
        val alignedTimeframes: Int,
        val availableTimeframes: Int,
        val breakoutLevel: Double,
        val conditions: String,
        val mfe: Double,
        val mae: Double,
        val lastPrice: Double?
    )

    data class ExplosionSummary(val total: Int, val pending: Int, val hits: Int, val fails: Int, val neutral: Int) {
        val accuracy: Double get() = if (hits + fails == 0) 0.0 else 100.0 * hits / (hits + fails)
    }

    data class PendingExplosionAlert(
        val uid: String,
        val symbol: String,
        val timeframe: String,
        val eventAt: Long,
        val direction: String,
        val price: Double,
        val volumeRatio: Double,
        val atrRatio: Double,
        val adx: Double,
        val alignedTimeframes: Int,
        val availableTimeframes: Int,
        val breakoutLevel: Double
    )

    @Synchronized fun insertExplosionIfNew(a: ExplosionAssessment): ExplosionInsertResult {
        val direction = a.direction ?: return ExplosionInsertResult(false, "")
        if (!a.confirmed) return ExplosionInsertResult(false, "")
        val uid = "${a.symbol}-${a.timeframe}-${a.candleCloseTime}-${direction.name}-EXP"
        val horizonMinutes = when (a.timeframe) {
            "15m" -> 240L
            "30m" -> 480L
            "1h" -> 1_440L
            "4h" -> 4_320L
            // Tres días caben íntegramente en una consulta Binance de 1.000
            // velas de 5m; no se declara NEUTRA con un tramo sin observar.
            "1d" -> 4_320L
            else -> 1_440L
        }
        val bullish = direction == ExplosionDirection.BULLISH
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("uid", uid); put("symbol", a.symbol); put("timeframe", a.timeframe); put("detected_ts", a.timestamp); put("candle_close", a.candleCloseTime)
                put("price", a.price); put("direction", direction.name); put("status", OutcomeStatus.PENDING.name); put("expiry_ts", a.candleCloseTime + horizonMinutes * 60_000L)
                put("target_price", if (bullish) a.price + a.atr else a.price - a.atr); put("stop_price", if (bullish) a.price - a.atr else a.price + a.atr)
                put("atr", a.atr); put("volume_ratio", a.volumeRatio); put("atr_ratio", a.atrExpansionRatio); put("adx", a.adx); put("previous_adx", a.previousAdx)
                put("aligned_timeframes", a.alignedTimeframes); put("available_timeframes", a.availableTimeframes); put("breakout_level", a.breakoutLevel)
                put("conditions", JSONArray(a.reasons).toString()); put("last_price", a.price)
            }
            val id = db.insertWithOnConflict("explosion_event", null, values, SQLiteDatabase.CONFLICT_IGNORE)
            if (id == -1L) return ExplosionInsertResult(false, uid)
            ContentValues().also { outbox ->
                outbox.put("explosion_uid", uid); outbox.put("created_ts", a.timestamp); outbox.put("status", "PENDING")
                db.insertOrThrow("explosion_alert_outbox", null, outbox)
            }
            db.setTransactionSuccessful()
            return ExplosionInsertResult(true, uid)
        } finally { db.endTransaction() }
    }

    fun pendingExplosionAlerts(limit: Int = 20): List<PendingExplosionAlert> {
        val out = mutableListOf<PendingExplosionAlert>()
        readableDatabase.rawQuery(
            """SELECT o.explosion_uid,e.symbol,e.timeframe,e.candle_close,e.direction,e.price,e.volume_ratio,e.atr_ratio,e.adx,e.aligned_timeframes,e.available_timeframes,e.breakout_level
                FROM explosion_alert_outbox o JOIN explosion_event e ON e.uid=o.explosion_uid
                WHERE o.status IN ('PENDING','FAILED') AND o.attempts<5 ORDER BY o.created_ts LIMIT ?""",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) out += PendingExplosionAlert(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getString(4), c.getDouble(5), c.getDouble(6), c.getDouble(7), c.getDouble(8), c.getInt(9), c.getInt(10), c.getDouble(11))
        }
        return out
    }

    fun markExplosionAlertSent(uid: String) {
        ContentValues().also { v ->
            v.put("status", "SENT"); v.put("sent_ts", System.currentTimeMillis()); v.put("last_error", "")
            writableDatabase.update("explosion_alert_outbox", v, "explosion_uid=?", arrayOf(uid))
        }
    }

    fun markExplosionAlertFailed(uid: String, error: String) {
        writableDatabase.execSQL("UPDATE explosion_alert_outbox SET status='FAILED',attempts=attempts+1,last_error=? WHERE explosion_uid=?", arrayOf(error.take(300), uid))
    }

    fun pendingExplosionStarts(): Map<String, Long> {
        val out = linkedMapOf<String, Long>()
        readableDatabase.rawQuery("SELECT symbol,MIN(candle_close+1) FROM explosion_event WHERE status='PENDING' GROUP BY symbol", null).use { c ->
            while (c.moveToNext()) out[c.getString(0)] = c.getLong(1)
        }
        return out
    }

    @Synchronized fun updateExplosionOutcomes(currentPrices: Map<String, Double>, histories: Map<String, List<Candle>>, now: Long = System.currentTimeMillis()) {
        val db = writableDatabase
        db.rawQuery("SELECT uid,symbol,candle_close,price,direction,atr,expiry_ts,mfe,mae FROM explosion_event WHERE status='PENDING'", null).use { c ->
            while (c.moveToNext()) {
                val uid = c.getString(0); val symbol = c.getString(1); val candleClose = c.getLong(2); val entry = c.getDouble(3)
                val direction = ExplosionDirection.valueOf(c.getString(4)); val atr = c.getDouble(5); val expiry = c.getLong(6)
                val selected = OutcomeTracker.candlesStrictlyAfter(candleClose, minOf(now, expiry), histories[symbol].orEmpty())
                val directionLabel = if (direction == ExplosionDirection.BULLISH) "BUY" else "SELL"
                val excursions = OutcomeTracker.excursions(entry, directionLabel, selected)
                val mfe = maxOf(c.getDouble(7), excursions.mfe); val mae = minOf(c.getDouble(8), excursions.mae)
                val touched = ExplosionDetector.resolve(entry, atr, direction, selected)
                val final = touched ?: if (now >= expiry) OutcomeStatus.NEUTRAL else null
                ContentValues().also { v ->
                    currentPrices[symbol]?.let { v.put("last_price", it) }; v.put("mfe", mfe); v.put("mae", mae)
                    if (final != null) { v.put("status", final.name); v.put("resolved_ts", now) }
                    db.update("explosion_event", v, "uid=?", arrayOf(uid))
                }
                if (final != null) log(symbol, "EXPLOSION_RESULT", "$uid => ${final.name} • MFE ${"%.2f".format(mfe)}% • MAE ${"%.2f".format(mae)}%")
            }
        }
    }

    fun explosionEvents(limit: Int = 200, symbol: String? = null): List<ExplosionEvent> {
        val out = mutableListOf<ExplosionEvent>()
        val where = if (symbol == null) "" else " WHERE symbol=?"
        val args = if (symbol == null) arrayOf(limit.toString()) else arrayOf(symbol, limit.toString())
        readableDatabase.rawQuery(
            """SELECT uid,symbol,timeframe,detected_ts,candle_close,price,direction,status,expiry_ts,resolved_ts,target_price,stop_price,atr,volume_ratio,atr_ratio,adx,previous_adx,aligned_timeframes,available_timeframes,breakout_level,conditions,mfe,mae,last_price
                FROM explosion_event$where ORDER BY detected_ts DESC LIMIT ?""", args
        ).use { c ->
            while (c.moveToNext()) out += ExplosionEvent(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4), c.getDouble(5), c.getString(6), c.getString(7), c.getLong(8), if (c.isNull(9)) null else c.getLong(9), c.getDouble(10), c.getDouble(11), c.getDouble(12), c.getDouble(13), c.getDouble(14), c.getDouble(15), c.getDouble(16), c.getInt(17), c.getInt(18), c.getDouble(19), c.getString(20), c.getDouble(21), c.getDouble(22), if (c.isNull(23)) null else c.getDouble(23))
        }
        return out
    }

    fun latestPendingExplosion(symbol: String, timeframe: String? = null): ExplosionEvent? =
        explosionEvents(50, symbol).firstOrNull { it.status == OutcomeStatus.PENDING.name && (timeframe == null || it.timeframe == timeframe) }

    fun explosionSummary(): ExplosionSummary {
        var total = 0; var pending = 0; var hits = 0; var fails = 0; var neutral = 0
        readableDatabase.rawQuery("SELECT status,COUNT(*) FROM explosion_event GROUP BY status", null).use { c ->
            while (c.moveToNext()) {
                val count = c.getInt(1); total += count
                when (c.getString(0)) { "PENDING" -> pending = count; "HIT" -> hits = count; "FAIL" -> fails = count; "NEUTRAL" -> neutral = count }
            }
        }
        return ExplosionSummary(total, pending, hits, fails, neutral)
    }

    fun log(symbol: String, type: String, message: String) {
        val v = ContentValues().apply { put("ts", System.currentTimeMillis()); put("symbol", symbol); put("type", type); put("message", message) }
        writableDatabase.insert("diagnostic", null, v)
        writableDatabase.execSQL("DELETE FROM diagnostic WHERE id NOT IN (SELECT id FROM diagnostic ORDER BY id DESC LIMIT 8000)")
    }

    fun dashboard(): List<DashboardItem> {
        val sql = """SELECT a.symbol,a.timeframe,a.price,a.direction,a.score,a.confidence,a.probability,a.risk,a.regime,a.state,a.ts,COALESCE(a.change_pct,0),
            (SELECT MAX(e.ts) FROM alert_event e WHERE e.symbol=a.symbol),
            (SELECT COUNT(*) FROM persistence_followup f JOIN signal s ON s.uid=f.signal_uid WHERE s.symbol=a.symbol AND f.status='PENDING')
            FROM analysis a
            WHERE a.data_quality='COMPLETE' AND a.id=(SELECT a2.id FROM analysis a2 WHERE a2.symbol=a.symbol AND a2.data_quality='COMPLETE' ORDER BY a2.ts DESC,a2.id DESC LIMIT 1)
            ORDER BY a.confidence DESC"""
        readableDatabase.rawQuery(sql, null).use { c ->
            val out = mutableListOf<DashboardItem>()
            while (c.moveToNext()) out += DashboardItem(c.getString(0), c.getString(1), c.getDouble(2), c.getString(3), c.getInt(4), c.getInt(5), if (c.isNull(6)) null else c.getInt(6), c.getString(7), c.getString(8), c.getString(9), c.getLong(10), c.getDouble(11), if (c.isNull(12)) null else c.getLong(12), c.getInt(13))
            return out
        }
    }

    fun latestAnalysis(symbol: String): Map<String, String>? {
        readableDatabase.rawQuery("""SELECT timeframe,price,change_pct,direction,score,confidence,probability,risk,regime,structure,rsi,adx,plus_di,minus_di,atr,atr_pct,ema20,ema50,ema200,ema20_slope,ema50_slope,volume_ratio,volume_expansion,support,resistance,support_low,support_high,resistance_low,resistance_high,mtf_score,mtf_summary,rejection,explanation,forecast_low,forecast_high,invalidation,comparable_cases,hist_probability,stat_confidence,trend_score,momentum_score,volume_score,structure_score,sr_score,mtf_component,volatility_score,historical_score,btc_score,fundamental_score,state,presignal,signal,data_quality,missing_data,ts FROM analysis WHERE symbol=? AND data_quality='COMPLETE' ORDER BY ts DESC LIMIT 1""", arrayOf(symbol)).use { c ->
            if (!c.moveToFirst()) return null
            val keys = listOf("timeframe","price","change_pct","direction","score","confidence","probability","risk","regime","structure","rsi","adx","plus_di","minus_di","atr","atr_pct","ema20","ema50","ema200","ema20_slope","ema50_slope","volume_ratio","volume_expansion","support","resistance","support_low","support_high","resistance_low","resistance_high","mtf_score","mtf_summary","rejection","explanation","forecast_low","forecast_high","invalidation","comparable_cases","hist_probability","stat_confidence","trend_score","momentum_score","volume_score","structure_score","sr_score","mtf_component","volatility_score","historical_score","btc_score","fundamental_score","state","presignal","signal","data_quality","missing_data","ts")
            return keys.associateWith { k -> val idx = c.getColumnIndex(k); if (c.isNull(idx)) "—" else c.getString(idx) }
        }
    }

    fun history(limit: Int = 100): List<String> {
        readableDatabase.rawQuery("SELECT symbol,timeframe,ts,price,direction,confidence,probability,status,persistence_status,legacy_status,mfe,mae,risk,outcome_quality,outcome_reason,target_price,stop_price FROM signal WHERE atr>0 ORDER BY ts DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            val out = mutableListOf<String>()
            val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            while (c.moveToNext()) {
                val prob = if (c.isNull(6)) "sin calibrar" else "P${c.getInt(6)}%"
                val operation = c.getString(7); val persistence = c.getString(8) ?: "PENDING"
                val legacy = if (c.isNull(9)) "" else " • clasificación anterior ${c.getString(9)}"
                val quality = if (c.getString(13) == "EXACT_FIRST_TOUCH_5M_V2") "primer toque 5m" else "reclasificación pendiente/heredada"
                out += "${c.getString(0)} ${c.getString(1)} • ${c.getString(4)} ${c.getInt(5)}% ($prob) • ${"%.6f".format(c.getDouble(3))} • Operación $operation (${c.getString(14)}) • Persistencia $persistence$legacy • objetivo ${"%.6f".format(c.getDouble(15))} / stop ${"%.6f".format(c.getDouble(16))} • MFE ${"%.2f".format(c.getDouble(10))}% / MAE ${"%.2f".format(c.getDouble(11))}% • riesgo ${c.getString(12)} • $quality • ${sdf.format(Date(c.getLong(2)))}"
            }
            return out
        }
    }

    fun diagnostics(limit: Int = 160): List<String> {
        readableDatabase.rawQuery("SELECT ts,symbol,type,message FROM diagnostic ORDER BY ts DESC LIMIT ?", arrayOf(limit.toString())).use { c ->
            val out = mutableListOf<String>()
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (c.moveToNext()) out += "${sdf.format(Date(c.getLong(0)))}  ${c.getString(1)}  ${c.getString(2)}  ${c.getString(3)}"
            return out
        }
    }

    fun performance(symbol: String? = null): PerformanceSummary {
        val where = if (symbol == null) " WHERE atr>0 AND outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2')" else " WHERE symbol=? AND atr>0 AND outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2')"
        val args = if (symbol == null) null else arrayOf(symbol)
        var total = 0; var pending = 0; var hits = 0; var fails = 0; var neutral = 0
        readableDatabase.rawQuery("SELECT status,COUNT(*) FROM signal$where GROUP BY status", args).use { c ->
            while (c.moveToNext()) {
                val n = c.getInt(1); total += n
                when (c.getString(0)) { OutcomeStatus.PENDING.name -> pending += n; OutcomeStatus.HIT.name -> hits += n; OutcomeStatus.FAIL.name -> fails += n; OutcomeStatus.NEUTRAL.name -> neutral += n }
            }
        }
        val resolved = hits + fails
        return PerformanceSummary(total, pending, hits, fails, neutral, if (resolved == 0) 0.0 else 100.0 * hits / resolved)
    }

    fun signalCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM signal WHERE atr>0", null).use { c -> c.moveToFirst(); c.getInt(0) }
    fun legacyOutcomeCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM signal WHERE atr>0 AND outcome_quality NOT IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2')", null).use { c -> c.moveToFirst(); c.getInt(0) }
    fun analysisCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM analysis WHERE data_quality='COMPLETE'", null).use { c -> c.moveToFirst(); c.getInt(0) }
    fun preSignalCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM analysis WHERE data_quality='COMPLETE' AND presignal=1 AND signal=0", null).use { c -> c.moveToFirst(); c.getInt(0) }
    fun alertCount(): Int = readableDatabase.rawQuery(
        "SELECT COUNT(*) FROM alert_outbox o JOIN signal s ON s.uid=o.signal_uid WHERE s.atr>0 AND o.status='SENT'",
        null
    ).use { c -> c.moveToFirst(); c.getInt(0) }


    data class AnalysisAuditRow(
        val ts: Long,
        val timeframe: String,
        val state: String,
        val direction: String,
        val score: Int,
        val confidence: Int,
        val probability: Int?,
        val regime: String,
        val rejection: String
    )

    data class FollowupRow(
        val horizonMin: Int,
        val status: String,
        val movePct: Double?,
        val checkedTs: Long?,
        val marketTs: Long?
    )

    data class SignalDetailRow(
        val uid: String,
        val ts: Long,
        val timeframe: String,
        val direction: String,
        val confidence: Int,
        val status: String,
        val persistenceStatus: String,
        val legacyStatus: String?,
        val outcomeReason: String,
        val targetPrice: Double?,
        val stopPrice: Double?,
        val outcomeResolvedTs: Long?,
        val outcomeQuality: String,
        val mfe: Double,
        val mae: Double,
        val followups: List<FollowupRow>,
        val snapshot: Map<String, String>
    )

    data class DiagnosticStats(
        val cycles: Int,
        val errors: Int,
        val rejections: Int,
        val duplicateSignals: Int,
        val followups: Int
    )

    data class EngineHealth(
        val lastCompletedCycle: Long?,
        val lastAnalysis: Long?,
        val analysesToday: Int,
        val activePriceAlarms: Int
    )

    data class AssetHealth(val symbol: String, val timeframe: String?, val state: String?, val lastAnalysis: Long?, val lastError: String?, val lastErrorAt: Long?)

    fun engineHealth(): EngineHealth {
        val cycle = readableDatabase.rawQuery("SELECT MAX(ts) FROM diagnostic WHERE type='CYCLE' AND message LIKE 'Fin%'", null).use { c -> c.moveToFirst(); if (c.isNull(0)) null else c.getLong(0) }
        val analysis = readableDatabase.rawQuery("SELECT MAX(ts) FROM analysis WHERE data_quality='COMPLETE'", null).use { c -> c.moveToFirst(); if (c.isNull(0)) null else c.getLong(0) }
        val today = readableDatabase.rawQuery("SELECT COUNT(*) FROM analysis WHERE data_quality='COMPLETE' AND date(ts/1000,'unixepoch','localtime')=date('now','localtime')", null).use { c -> c.moveToFirst(); c.getInt(0) }
        return EngineHealth(cycle, analysis, today, activePriceAlarmCount())
    }

    fun assetHealth(symbols: Set<String>): List<AssetHealth> = symbols.sorted().map { symbol ->
        val latest = readableDatabase.rawQuery("SELECT timeframe,state,ts FROM analysis WHERE symbol=? AND data_quality='COMPLETE' ORDER BY ts DESC LIMIT 1", arrayOf(symbol)).use { c -> if (c.moveToFirst()) Triple(c.getString(0), c.getString(1), c.getLong(2)) else null }
        val error = readableDatabase.rawQuery("SELECT message,ts FROM diagnostic WHERE symbol=? AND type='ERROR' ORDER BY ts DESC LIMIT 1", arrayOf(symbol)).use { c -> if (c.moveToFirst()) c.getString(0) to c.getLong(1) else null }
        AssetHealth(symbol, latest?.first, latest?.second, latest?.third, error?.first, error?.second)
    }

    fun recentAnalyses(symbol: String, limit: Int = 12): List<AnalysisAuditRow> {
        val out = mutableListOf<AnalysisAuditRow>()
        readableDatabase.rawQuery(
            "SELECT ts,timeframe,state,direction,score,confidence,probability,regime,rejection FROM analysis WHERE symbol=? AND data_quality='COMPLETE' ORDER BY ts DESC LIMIT ?",
            arrayOf(symbol, limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out += AnalysisAuditRow(
                    ts = c.getLong(0), timeframe = c.getString(1), state = c.getString(2), direction = c.getString(3),
                    score = c.getInt(4), confidence = c.getInt(5), probability = if (c.isNull(6)) null else c.getInt(6),
                    regime = c.getString(7), rejection = c.getString(8) ?: ""
                )
            }
        }
        return out
    }

    fun recentSignals(symbol: String, limit: Int = 8): List<SignalDetailRow> {
        val db = readableDatabase
        val out = mutableListOf<SignalDetailRow>()
        db.rawQuery(
            "SELECT uid,ts,timeframe,direction,confidence,status,persistence_status,legacy_status,outcome_reason,target_price,stop_price,outcome_resolved_ts,outcome_quality,mfe,mae FROM signal WHERE symbol=? AND atr>0 ORDER BY ts DESC LIMIT ?",
            arrayOf(symbol, limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val uid = c.getString(0)
                val f = mutableListOf<FollowupRow>()
                val snapshot = signalSnapshot(uid)
                db.rawQuery(
                    "SELECT horizon_min,status,move_pct,checked_ts,market_ts FROM persistence_followup WHERE signal_uid=? ORDER BY horizon_min",
                    arrayOf(uid)
                ).use { fc ->
                    while (fc.moveToNext()) {
                        f += FollowupRow(
                            horizonMin = fc.getInt(0), status = fc.getString(1),
                            movePct = if (fc.isNull(2)) null else fc.getDouble(2),
                            checkedTs = if (fc.isNull(3)) null else fc.getLong(3),
                            marketTs = if (fc.isNull(4)) null else fc.getLong(4)
                        )
                    }
                }
                out += SignalDetailRow(uid, c.getLong(1), c.getString(2), c.getString(3), c.getInt(4), c.getString(5), c.getString(6), if (c.isNull(7)) null else c.getString(7), c.getString(8) ?: "", if (c.isNull(9)) null else c.getDouble(9), if (c.isNull(10)) null else c.getDouble(10), if (c.isNull(11)) null else c.getLong(11), c.getString(12), c.getDouble(13), c.getDouble(14), f, snapshot)
            }
        }
        return out
    }

    private fun signalSnapshot(uid: String): Map<String, String> {
        val keys = listOf(
            "price","score","confidence","probability","risk","rsi","rsi_slope","rsi_bull_div","rsi_bear_div","adx","plus_di","minus_di","atr","atr_pct",
            "ema20","ema50","ema200","ema20_slope","ema50_slope","volume_ratio","volume_expansion","support","resistance","support_low","support_high",
            "resistance_low","resistance_high","support_touches","resistance_touches","support_strength","resistance_strength","structure","regime","breakout_up",
            "breakout_down","false_breakout","expected_move_atr","mtf_score","mtf_summary","btc_regime","trend_score","momentum_score","volume_score","structure_score",
            "sr_score","mtf_component","volatility_score","historical_score","btc_score","fundamental_score","fundamental_headlines","fundamental_positive","fundamental_negative",
            "fundamental_summary","comparable_cases","calibration_wins","calibration_losses","calibration_neutral","hist_probability","stat_confidence","forecast_low","forecast_high",
            "invalidation","decision_threshold","closed_candle","allow_buy","allow_sell","explanation","missing_data","snapshot_quality"
        )
        readableDatabase.rawQuery("SELECT ${keys.joinToString(",")} FROM signal_snapshot WHERE signal_uid=?", arrayOf(uid)).use { c ->
            if (!c.moveToFirst()) return emptyMap()
            return keys.mapIndexed { index, key -> key to if (c.isNull(index)) "—" else c.getString(index) }.toMap()
        }
    }

    fun diagnosticStats(): DiagnosticStats {
        fun count(type: String): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM diagnostic WHERE type=?", arrayOf(type)).use { c -> c.moveToFirst(); c.getInt(0) }
        val completedCycles = readableDatabase.rawQuery("SELECT COUNT(*) FROM diagnostic WHERE type='CYCLE' AND message LIKE 'Fin%'", null).use { c -> c.moveToFirst(); c.getInt(0) }
        val currentDuplicates = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM diagnostic WHERE type='DUPLICATE' AND ts >= COALESCE((SELECT MIN(ts) FROM analysis WHERE data_quality='COMPLETE'),0)",
            null
        ).use { c -> c.moveToFirst(); c.getInt(0) }
        val rejections = readableDatabase.rawQuery("SELECT COUNT(*) FROM analysis WHERE data_quality='COMPLETE' AND rejection IS NOT NULL AND TRIM(rejection)<>''", null).use { c -> c.moveToFirst(); c.getInt(0) }
        return DiagnosticStats(completedCycles, count("ERROR"), rejections, currentDuplicates, count("FOLLOWUP") + count("PERSISTENCE"))
    }

    fun performanceByTimeframe(): Map<String, PerformanceSummary> {
        val out = linkedMapOf<String, PerformanceSummary>()
        listOf("15m","30m","1h","4h","1d").forEach { tf ->
            val c = readableDatabase.rawQuery("SELECT COUNT(*),SUM(CASE WHEN status='PENDING' THEN 1 ELSE 0 END),SUM(CASE WHEN status='HIT' THEN 1 ELSE 0 END),SUM(CASE WHEN status='FAIL' THEN 1 ELSE 0 END),SUM(CASE WHEN status='NEUTRAL' THEN 1 ELSE 0 END) FROM signal WHERE timeframe=? AND atr>0 AND outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2')", arrayOf(tf))
            c.use {
                if (it.moveToFirst()) {
                    val total = it.getInt(0); val pending = it.getInt(1); val hits = it.getInt(2); val fails = it.getInt(3); val neutral = it.getInt(4)
                    if (total > 0) {
                        val resolved = hits + fails
                        out[tf] = PerformanceSummary(total, pending, hits, fails, neutral, if (resolved > 0) hits * 100.0 / resolved else 0.0)
                    }
                }
            }
        }
        return out
    }

    fun performanceByDirection(): Map<String, PerformanceSummary> = linkedMapOf(
        "BUY" to performanceFor("s.direction LIKE '%BUY%'"),
        "SELL" to performanceFor("s.direction LIKE '%SELL%'")
    ).filterValues { it.totalSignals > 0 }

    fun performanceByRegime(): Map<String, PerformanceSummary> {
        val out = linkedMapOf<String, PerformanceSummary>()
        readableDatabase.rawQuery(
            """SELECT ss.regime FROM signal s JOIN signal_snapshot ss ON ss.signal_uid=s.uid
                WHERE s.atr>0 AND s.outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2')
                GROUP BY ss.regime ORDER BY COUNT(*) DESC""", null
        ).use { c -> while (c.moveToNext()) {
            val regime = c.getString(0)
            out[regime] = performanceFor("ss.regime=?", arrayOf(regime), joinSnapshot = true)
        } }
        return out
    }

    fun performanceByRisk(): Map<String, PerformanceSummary> {
        val out = linkedMapOf<String, PerformanceSummary>()
        readableDatabase.rawQuery(
            "SELECT risk FROM signal WHERE atr>0 AND outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2') GROUP BY risk ORDER BY risk", null
        ).use { c -> while (c.moveToNext()) {
            val risk = c.getString(0)
            out[risk] = performanceFor("s.risk=?", arrayOf(risk))
        } }
        return out
    }

    private fun performanceFor(extraWhere: String, args: Array<String>? = null, joinSnapshot: Boolean = false): PerformanceSummary {
        val join = if (joinSnapshot) " JOIN signal_snapshot ss ON ss.signal_uid=s.uid" else ""
        val sql = """SELECT COUNT(*),
            SUM(CASE WHEN s.status='PENDING' THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.status='HIT' THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.status='FAIL' THEN 1 ELSE 0 END),
            SUM(CASE WHEN s.status='NEUTRAL' THEN 1 ELSE 0 END)
            FROM signal s$join WHERE s.atr>0 AND s.outcome_quality IN ('EXACT_FIRST_TOUCH_5M_V2','RECLASSIFY_FIRST_TOUCH_V2') AND $extraWhere"""
        return readableDatabase.rawQuery(sql, args).use { c ->
            c.moveToFirst()
            val total = c.getInt(0); val pending = c.getInt(1); val hits = c.getInt(2); val fails = c.getInt(3); val neutral = c.getInt(4)
            val resolved = hits + fails
            PerformanceSummary(total, pending, hits, fails, neutral, if (resolved == 0) 0.0 else hits * 100.0 / resolved)
        }
    }

    fun writeCsv(writer: Writer) {
        writer.write("\uFEFFseccion;registro;campo;valor\n")
        fullExportQueries().forEach { (section, sql) ->
            readableDatabase.rawQuery(sql, null).use { c ->
                var row = 0
                while (c.moveToNext()) {
                    row++
                    c.columnNames.forEachIndexed { index, name ->
                        writer.write(csvCell(section)); writer.write(';'.code); writer.write(row.toString()); writer.write(';'.code)
                        writer.write(csvCell(name)); writer.write(';'.code)
                        writer.write(csvCell(if (c.isNull(index)) "" else c.getString(index))); writer.write('\n'.code)
                    }
                }
                if (row == 0) writer.write("${csvCell(section)};0;${csvCell("estado")};${csvCell("Sin registros")}\n")
            }
        }
        writer.flush()
    }

    fun writeAuditText(writer: Writer) {
        writer.write("CRIPTOANÁLISIS MULTI — EXPORTACIÓN GLOBAL COMPLETA\n")
        writer.write("====================================================\n")
        writer.write("Generada: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        fullExportQueries().forEach { (section, sql) ->
            writer.write("\n$section\n${"-".repeat(section.length.coerceAtLeast(3))}\n")
            readableDatabase.rawQuery(sql, null).use { c ->
                var row = 0
                while (c.moveToNext()) {
                    row++
                    writer.write("\nREGISTRO $row\n")
                    c.columnNames.forEachIndexed { index, name -> writer.write("$name: ${if (c.isNull(index)) "N/D" else c.getString(index)}\n") }
                }
                if (row == 0) writer.write("Sin registros.\n")
            }
        }
        writer.flush()
    }

    fun writeRtf(writer: Writer) {
        writer.write("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Arial;}}\\fs20 ")
        writeAuditText(RtfEscapingWriter(writer))
        writer.write('}'.code)
        writer.flush()
    }

    fun exportCsv(): String = StringWriter().also(::writeCsv).toString()
    fun exportAuditText(): String = StringWriter().also(::writeAuditText).toString()
    fun exportRtf(): String = StringWriter().also(::writeRtf).toString()

    private fun fullExportQueries(): List<Pair<String, String>> = listOf(
        "CONFIGURACIÓN GENERAL" to "SELECT key,value,updated_ts FROM app_setting ORDER BY key",
        "CRIPTOMONEDAS Y CONFIGURACIÓN" to "SELECT a.symbol,a.name,a.enabled,a.updated_ts,c.timeframes,c.threshold,c.allow_buy,c.allow_sell,c.sound,c.vibration,c.updated_ts AS config_updated_ts FROM crypto_asset a LEFT JOIN watch_config c ON c.symbol=a.symbol ORDER BY a.symbol",
        "EVALUACIONES" to "SELECT * FROM analysis ORDER BY ts DESC",
        "SEÑALES" to "SELECT * FROM signal ORDER BY ts DESC",
        "SNAPSHOTS EXACTOS" to "SELECT * FROM signal_snapshot ORDER BY ts DESC",
        "SEGUIMIENTOS ANTERIORES V1 CONSERVADOS" to "SELECT * FROM followup ORDER BY due_ts DESC",
        "PERSISTENCIA ADAPTADA V2" to "SELECT * FROM persistence_followup ORDER BY due_ts DESC",
        "EVENTOS DE ALERTA" to "SELECT * FROM alert_event ORDER BY ts DESC",
        "OUTBOX DE ALERTAS" to "SELECT * FROM alert_outbox ORDER BY created_ts DESC",
        "ALARMAS DE PRECIO" to "SELECT * FROM price_alarm ORDER BY created_ts DESC",
        "POSIBLES EXPLOSIONES (MÓDULO INDEPENDIENTE)" to "SELECT * FROM explosion_event ORDER BY detected_ts DESC",
        "OUTBOX DE EXPLOSIONES" to "SELECT * FROM explosion_alert_outbox ORDER BY created_ts DESC",
        "DIAGNÓSTICO" to "SELECT * FROM diagnostic ORDER BY ts DESC"
    )

    private fun csvCell(value: String) = "\"${value.replace("\"", "\"\"").replace("\r\n", "\n").replace('\r', '\n')}\""

    private class RtfEscapingWriter(private val output: Writer) : Writer() {
        override fun write(buffer: CharArray, offset: Int, length: Int) {
            for (index in offset until offset + length) write(buffer[index].code)
        }

        override fun write(value: Int) {
            val ch = value.toChar()
            when (ch) {
                '\\' -> output.write("\\\\")
                '{' -> output.write("\\{")
                '}' -> output.write("\\}")
                '\n' -> output.write("\\par\n")
                '\r' -> Unit
                else -> if (ch.code in 32..126) output.write(ch.code) else {
                    val signedCode = if (ch.code > 32767) ch.code - 65536 else ch.code
                    output.write("\\u$signedCode?")
                }
            }
        }

        override fun flush() = output.flush()
        override fun close() = Unit
    }

    companion object {
        private const val MIN_CALIBRATION_CASES = 10
        @Volatile private var instance: AppDatabase? = null
        fun get(c: Context) = instance ?: synchronized(this) { instance ?: AppDatabase(c.applicationContext).also { instance = it } }
    }
}
