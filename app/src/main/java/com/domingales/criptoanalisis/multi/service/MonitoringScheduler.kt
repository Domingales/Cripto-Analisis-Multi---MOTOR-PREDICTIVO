package com.domingales.criptoanalisis.multi.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.util.Prefs
import com.google.firebase.messaging.RemoteMessage

object MonitoringScheduler {
    private const val PERIODIC_JOB_ID = 4310
    private const val IMMEDIATE_JOB_ID = 4311
    private const val MIN_PERIODIC_MINUTES = 15

    fun enable(context: Context) {
        Prefs.setWatching(context, true)
        schedulePeriodic(context)
        startCoordinator(context, MarketWatchService.ACTION_START, true)
        HybridRemoteClient.register(context)
    }

    fun disable(context: Context) {
        Prefs.setWatching(context, false)
        context.getSystemService(JobScheduler::class.java).apply {
            cancel(PERIODIC_JOB_ID)
            cancel(IMMEDIATE_JOB_ID)
        }
        context.stopService(Intent(context, MarketWatchService::class.java))
        HybridRemoteClient.unregister(context)
    }

    fun restoreAfterBoot(context: Context) {
        if (!Prefs.watching(context)) return
        schedulePeriodic(context)
        startCoordinator(context, MarketWatchService.ACTION_RESTORE, true)
        HybridRemoteClient.register(context)
    }

    fun reconfigure(context: Context) {
        if (!Prefs.watching(context)) return
        schedulePeriodic(context)
        startCoordinator(context, MarketWatchService.ACTION_RECONFIGURE, false)
        HybridRemoteClient.register(context)
    }

    /** Compatibilidad con llamadas anteriores: ahora despierta el coordinador permanente. */
    fun scheduleNow(context: Context) {
        if (!Prefs.watching(context)) return
        startCoordinator(context, MarketWatchService.ACTION_REMOTE, false)
    }

    fun remoteTrigger(context: Context, requestedAt: Long, receivedAt: Long, priority: Int, originalPriority: Int) {
        if (!Prefs.watching(context)) return
        if (priority != RemoteMessage.PRIORITY_HIGH) {
            AppDatabase.get(context).log("SYSTEM", "REMOTE", "FCM degradado a prioridad $priority/$originalPriority; se usa JobScheduler de respaldo")
            scheduleImmediateBackup(context)
            return
        }
        val intent = Intent(context, MarketWatchService::class.java)
            .setAction(MarketWatchService.ACTION_REMOTE)
            .putExtra(MarketWatchService.EXTRA_REQUESTED_AT, requestedAt)
            .putExtra(MarketWatchService.EXTRA_RECEIVED_AT, receivedAt)
            .putExtra(MarketWatchService.EXTRA_PRIORITY, priority)
            .putExtra(MarketWatchService.EXTRA_ORIGINAL_PRIORITY, originalPriority)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (t: Throwable) {
            AppDatabase.get(context).log("SYSTEM", "REMOTE", "No se pudo activar el coordinador: ${t.message}; se solicita respaldo JobScheduler")
            scheduleImmediateBackup(context)
        }
    }

    private fun startCoordinator(context: Context, action: String, runImmediately: Boolean) {
        val intent = Intent(context, MarketWatchService::class.java)
            .setAction(action)
            .putExtra(MarketWatchService.EXTRA_RUN_IMMEDIATELY, runImmediately)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (t: Throwable) {
            AppDatabase.get(context).log("SYSTEM", "SERVICE", "Inicio del servicio aplazado por Android: ${t.message}")
            scheduleImmediateBackup(context)
        }
    }

    private fun scheduleImmediateBackup(context: Context) {
        if (!Prefs.watching(context)) return
        val job = JobInfo.Builder(IMMEDIATE_JOB_ID, ComponentName(context, MarketScanJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(1_000L)
            .setOverrideDeadline(30_000L)
            .build()
        context.getSystemService(JobScheduler::class.java).schedule(job)
    }

    private fun schedulePeriodic(context: Context) {
        val minutes = backupMinutes(context)
        val job = JobInfo.Builder(PERIODIC_JOB_ID, ComponentName(context, MarketScanJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(minutes * 60_000L)
            .build()
        context.getSystemService(JobScheduler::class.java).schedule(job)
    }

    fun effectiveLocalMinutes(context: Context): Int = Prefs.intervalMinutes(context)
    fun backupMinutes(context: Context): Int = maxOf(MIN_PERIODIC_MINUTES, Prefs.intervalMinutes(context))
}
