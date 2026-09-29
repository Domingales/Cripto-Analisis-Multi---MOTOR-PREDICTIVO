package com.domingales.criptoanalisis.multi.util

import android.content.Context
import com.domingales.criptoanalisis.multi.domain.Assets
import java.util.UUID

object Prefs {
    private const val FILE = "crypto_multi_prefs"
    private fun p(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun watchlist(c: Context): Set<String> = p(c).getStringSet("watchlist", null) ?: setOf("BTC", "ETH", "ADA", "SOL", "XRP")
    fun setWatchlist(c: Context, symbols: Set<String>) = p(c).edit().putStringSet("watchlist", symbols).apply()

    fun favorites(c: Context): Set<String> = p(c).getStringSet("favorites", emptySet()) ?: emptySet()
    fun setFavorites(c: Context, symbols: Set<String>) = p(c).edit().putStringSet("favorites", symbols).apply()

    fun catalogSymbols(c: Context): Set<String> = p(c).getStringSet("catalog_symbols", emptySet()) ?: emptySet()
    fun setCatalogSymbols(c: Context, symbols: Set<String>) = p(c).edit().putStringSet("catalog_symbols", symbols).apply()

    fun watching(c: Context) = p(c).getBoolean("watching", false)
    fun setWatching(c: Context, value: Boolean) = p(c).edit().putBoolean("watching", value).apply()

    fun setServiceHeartbeat(c: Context, at: Long = System.currentTimeMillis()) = p(c).edit().putLong("service_heartbeat", at).apply()
    fun serviceHeartbeat(c: Context) = p(c).getLong("service_heartbeat", 0L)
    fun serviceResponsive(c: Context): Boolean {
        val heartbeat = serviceHeartbeat(c)
        return watching(c) && heartbeat > 0L && System.currentTimeMillis() - heartbeat < 150_000L
    }

    @Synchronized
    fun tryAcquireCycle(c: Context, origin: String, force: Boolean): Boolean {
        val now = System.currentTimeMillis()
        val preferences = p(c)
        val leaseUntil = preferences.getLong("cycle_lease_until", 0L)
        if (leaseUntil > now) return false
        val lastStarted = preferences.getLong("cycle_last_started", 0L)
        val duplicateWindow = maxOf(30_000L, minOf(120_000L, intervalMinutes(c) * 20_000L))
        if (!force && now - lastStarted < duplicateWindow) return false
        return preferences.edit()
            .putLong("cycle_lease_until", now + 15 * 60_000L)
            .putLong("cycle_last_started", now)
            .putString("cycle_last_origin", origin)
            .commit()
    }

    fun releaseCycle(c: Context, success: Boolean) = p(c).edit()
        .putLong("cycle_lease_until", 0L)
        .putLong("cycle_last_finished", System.currentTimeMillis())
        .putBoolean("cycle_last_success", success)
        .apply()

    fun lastCycleOrigin(c: Context) = p(c).getString("cycle_last_origin", "N/D") ?: "N/D"
    fun lastCycleStarted(c: Context) = p(c).getLong("cycle_last_started", 0L)
    fun lastCycleFinished(c: Context) = p(c).getLong("cycle_last_finished", 0L)

    fun timeframe(c: Context) = p(c).getString("timeframe", "1h") ?: "1h"
    fun setTimeframe(c: Context, v: String) = p(c).edit().putString("timeframe", v).apply()

    fun threshold(c: Context) = p(c).getInt("threshold", 80)
    fun setThreshold(c: Context, v: Int) = p(c).edit().putInt("threshold", v.coerceIn(55, 95)).apply()

    fun intervalMinutes(c: Context) = p(c).getInt("interval_minutes", 5)
    fun setIntervalMinutes(c: Context, v: Int) = p(c).edit().putInt("interval_minutes", v.coerceIn(1, 60)).apply()

    fun cooldownMinutes(c: Context) = p(c).getInt("cooldown_minutes", 60)
    fun setCooldownMinutes(c: Context, v: Int) = p(c).edit().putInt("cooldown_minutes", v.coerceIn(15, 1440)).apply()

    fun closedCandleMode(c: Context) = p(c).getBoolean("closed_candle", true)
    fun setClosedCandleMode(c: Context, v: Boolean) = p(c).edit().putBoolean("closed_candle", v).apply()

    fun newsEnabled(c: Context) = p(c).getBoolean("news_enabled", true)
    fun setNewsEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("news_enabled", v).apply()

    fun soundEnabled(c: Context) = p(c).getBoolean("sound_enabled", true)
    fun setSoundEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("sound_enabled", v).apply()

    fun vibrationEnabled(c: Context) = p(c).getBoolean("vibration_enabled", true)
    fun setVibrationEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("vibration_enabled", v).apply()

    fun explosionDetectorEnabled(c: Context) = p(c).getBoolean("explosion_detector_enabled", true)
    fun setExplosionDetectorEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("explosion_detector_enabled", v).apply()

    fun explosionSoundEnabled(c: Context) = p(c).getBoolean("explosion_sound_enabled", true)
    fun setExplosionSoundEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("explosion_sound_enabled", v).apply()

    fun explosionVibrationEnabled(c: Context) = p(c).getBoolean("explosion_vibration_enabled", true)
    fun setExplosionVibrationEnabled(c: Context, v: Boolean) = p(c).edit().putBoolean("explosion_vibration_enabled", v).apply()

    fun symbolSoundEnabled(c: Context, symbol: String): Boolean = if (p(c).contains("sound_$symbol")) p(c).getBoolean("sound_$symbol", true) else soundEnabled(c)
    fun setSymbolSoundEnabled(c: Context, symbol: String, v: Boolean) = p(c).edit().putBoolean("sound_$symbol", v).apply()

    fun symbolVibrationEnabled(c: Context, symbol: String): Boolean = if (p(c).contains("vibration_$symbol")) p(c).getBoolean("vibration_$symbol", true) else vibrationEnabled(c)
    fun setSymbolVibrationEnabled(c: Context, symbol: String, v: Boolean) = p(c).edit().putBoolean("vibration_$symbol", v).apply()

    fun menuOnLeft(c: Context) = p(c).getBoolean("menu_on_left", true)
    fun setMenuOnLeft(c: Context, v: Boolean) = p(c).edit().putBoolean("menu_on_left", v).apply()

    fun applyV371MenuOnRight(c: Context) {
        if (!p(c).getBoolean("v371_menu_right_applied", false)) {
            p(c).edit()
                .putBoolean("menu_on_left", false)
                .putBoolean("v371_menu_right_applied", true)
                .apply()
        }
    }

    fun dashboardSort(c: Context) = p(c).getString("dashboard_sort", "CONFIDENCE") ?: "CONFIDENCE"
    fun setDashboardSort(c: Context, v: String) = p(c).edit().putString("dashboard_sort", v).apply()

    fun remoteEndpoint(c: Context) = p(c).getString("remote_endpoint", "")?.trim().orEmpty().trimEnd('/')
    fun setRemoteEndpoint(c: Context, v: String) = p(c).edit().putString("remote_endpoint", v.trim().trimEnd('/')).apply()

    fun remoteRegistrationKey(c: Context) = p(c).getString("remote_registration_key", "")?.trim().orEmpty()
    fun setRemoteRegistrationKey(c: Context, v: String) = p(c).edit().putString("remote_registration_key", v.trim()).apply()

    fun installationId(c: Context): String {
        val existing = p(c).getString("installation_id", null)
        if (!existing.isNullOrBlank()) return existing
        return UUID.randomUUID().toString().also { p(c).edit().putString("installation_id", it).commit() }
    }

    fun symbolTimeframes(c: Context, symbol: String): Set<String> = p(c).getStringSet("tf_$symbol", null) ?: setOf(timeframe(c), "4h").filter { it in setOf("15m","30m","1h","4h","1d") }.toSet()
    fun setSymbolTimeframes(c: Context, symbol: String, values: Set<String>) = p(c).edit().putStringSet("tf_$symbol", values.ifEmpty { setOf(timeframe(c)) }).apply()

    fun symbolThreshold(c: Context, symbol: String): Int = p(c).getInt("th_$symbol", threshold(c)).coerceIn(55, 95)
    fun setSymbolThreshold(c: Context, symbol: String, v: Int) = p(c).edit().putInt("th_$symbol", v.coerceIn(55, 95)).apply()

    fun symbolAllowBuy(c: Context, symbol: String) = p(c).getBoolean("buy_$symbol", true)
    fun setSymbolAllowBuy(c: Context, symbol: String, v: Boolean) = p(c).edit().putBoolean("buy_$symbol", v).apply()

    fun symbolAllowSell(c: Context, symbol: String) = p(c).getBoolean("sell_$symbol", true)
    fun setSymbolAllowSell(c: Context, symbol: String, v: Boolean) = p(c).edit().putBoolean("sell_$symbol", v).apply()

    fun allSymbols() = Assets.supported.map { it.symbol }
}
