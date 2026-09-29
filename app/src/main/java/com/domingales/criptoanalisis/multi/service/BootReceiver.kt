package com.domingales.criptoanalisis.multi.service

import android.content.BroadcastReceiver
import android.content.Context
import com.domingales.criptoanalisis.multi.util.Prefs

class BootReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:android.content.Intent){
        if ((intent.action == android.content.Intent.ACTION_BOOT_COMPLETED || intent.action == android.content.Intent.ACTION_MY_PACKAGE_REPLACED) && Prefs.watching(context)) {
            // Restaura el coordinador permanente y el trabajo periódico de respaldo.
            MonitoringScheduler.restoreAfterBoot(context)
        }
    }
}
