package com.domingales.criptoanalisis.multi

import android.app.Application
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.service.MonitoringScheduler
import com.domingales.criptoanalisis.multi.service.NotificationHelper
import com.domingales.criptoanalisis.multi.util.Prefs

class CryptoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        AppDatabase.get(this).syncConfiguration(this)
        if (Prefs.watching(this)) MonitoringScheduler.restoreAfterBoot(this)
    }
}
