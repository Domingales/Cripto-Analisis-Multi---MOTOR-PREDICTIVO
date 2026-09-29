package com.domingales.criptoanalisis.multi.service

import android.app.job.JobParameters
import android.app.job.JobService
import com.domingales.criptoanalisis.multi.util.Prefs

class MarketScanJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        if (!Prefs.watching(this)) return false
        val started = MarketScanRunner.start(this, origin = "JOB_RESPALDO") { retry -> jobFinished(params, retry) }
        return started
    }

    override fun onStopJob(params: JobParameters): Boolean = Prefs.watching(this)
}
