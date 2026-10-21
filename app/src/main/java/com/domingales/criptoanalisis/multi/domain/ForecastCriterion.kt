package com.domingales.criptoanalisis.multi.domain

/** Evaluación adicional; no cambia señales, probabilidades ni aprendizaje V2. */
object ForecastCriterion {
    const val PROFILE = "MAGNITUDE_TIME_V1"
    const val STEP = 300_000L
    const val DAY = 86_400_000L
    enum class Status { PENDING, HIT, APPROXIMATE, LATE, FAIL }
    data class Contract(
        val entry: Double, val direction: Direction, val issuedAt: Long, val targetPct: Double,
        val horizonMillis: Long = DAY, val graceMillis: Long = 2 * DAY,
        val expiryMillis: Long = 5 * DAY, val minimumFraction: Double = 0.75
    ) {
        init {
            require(entry.isFinite() && entry > 0 && targetPct.isFinite() && targetPct > 0)
            require(direction != Direction.WAIT && issuedAt >= 0)
            require(horizonMillis >= STEP && graceMillis >= horizonMillis && expiryMillis >= graceMillis)
            require(minimumFraction.isFinite() && minimumFraction > 0 && minimumFraction <= 1)
        }
        // Sólo velas completas que empiezan después de registrar el pronóstico.
        val firstOpen: Long get() = (issuedAt / STEP + 1) * STEP
    }
    data class Result(
        val status: Status, val reason: String, val firstTargetAt: Long? = null,
        val firstApproximateAt: Long? = null, val mfe: Double = 0.0, val mae: Double = 0.0
    )

    fun evaluate(contract: Contract, history: List<Candle>, now: Long): Result {
        val end = minOf(now, contract.issuedAt + contract.expiryMillis)
        if (end < contract.firstOpen + STEP - 1) return Result(Status.PENDING, "WAITING_CLOSED_5M")
        val selected = history.filter { it.openTime >= contract.firstOpen && it.closeTime <= end }.sortedBy { it.openTime }
        // Un hueco invalida la cronología posterior: nunca premiar un toque tras él.
        val prefix = mutableListOf<Candle>()
        var expected = contract.firstOpen
        for (c in selected) {
            if (c.openTime != expected || c.closeTime != c.openTime + STEP - 1 ||
                !listOf(c.open, c.high, c.low, c.close).all { it.isFinite() && it > 0 } ||
                c.high < maxOf(c.open, c.close, c.low) || c.low > minOf(c.open, c.close)) break
            prefix += c
            expected += STEP
        }
        val sell = contract.direction == Direction.SELL || contract.direction == Direction.REVERSAL_SELL
        fun favorable(c: Candle) = (if (sell) contract.entry - c.low else c.high - contract.entry) / contract.entry * 100
        val excursions = OutcomeTracker.excursions(contract.entry, contract.direction.name, prefix)
        val exact = prefix.firstOrNull { favorable(it) + 1e-9 >= contract.targetPct }?.closeTime
        val approximate = prefix.firstOrNull { favorable(it) + 1e-9 >= contract.targetPct * contract.minimumFraction }?.closeTime
        fun result(status: Status, reason: String) = Result(status, reason, exact, approximate, excursions.mfe, excursions.mae)
        val onTime = contract.issuedAt + contract.horizonMillis
        val grace = contract.issuedAt + contract.graceMillis
        val expiry = contract.issuedAt + contract.expiryMillis
        // Última vela completa que cabe dentro de cada plazo; resolución de 5 minutos.
        fun covered(deadline: Long): Boolean {
            val requiredLastOpen = ((deadline + 1) / STEP) * STEP - STEP
            return now >= deadline && prefix.lastOrNull()?.openTime?.let { it >= requiredLastOpen } == true
        }
        if (exact != null && exact <= onTime) return result(Status.HIT, "TARGET_WITHIN_24H")
        if (!covered(onTime)) return result(Status.PENDING, "WAITING_24H_OR_MISSING_5M")
        if (approximate != null && approximate <= grace) return result(Status.APPROXIMATE, "AT_LEAST_75_PERCENT_WITHIN_48H")
        if (!covered(grace)) return result(Status.PENDING, "WAITING_48H_OR_MISSING_5M")
        if (approximate != null && approximate <= expiry) return result(Status.LATE, "SUFFICIENT_MOVE_AFTER_GRACE")
        if (!covered(expiry)) return result(Status.PENDING, "WAITING_120H_OR_MISSING_5M")
        return result(Status.FAIL, "INSUFFICIENT_MOVE_WITHIN_120H")
    }
}
