package com.project.lol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.toColorInt
import com.project.lol.R
import com.project.lol.offline.DownloadManager
import java.io.File

class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "spotilol_downloads"
        private const val NOTIF_ID = 3

        const val ACTION_SKIP = "com.project.lol.download.ACTION_SKIP"
        const val ACTION_CANCEL = "com.project.lol.download.ACTION_CANCEL"
    }

    private val handler = Handler(Looper.getMainLooper())

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!DownloadManager.isDownloading() && !DownloadManager.isWorkPending()) {
                stopSelf()
                return
            }
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        runCatching {
            val dir = File(filesDir, "downloads")
            if (dir.exists()) dir.listFiles()?.forEach { if (it.name.endsWith(".part")) it.delete() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SKIP -> DownloadManager.skipCurrent()
            ACTION_CANCEL -> DownloadManager.cancelAll()
        }
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(), foregroundServiceType()
            )
        } catch (e: Throwable) {
            android.util.Log.e("DownloadService", "startForeground failed", e)
        }
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Spotilol track downloads"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val pct = DownloadManager.lastPct
        val label = DownloadManager.lastLabel.ifBlank { "Preparing download..." }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Spotilol Download")
            .setContentText(label)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, pct.coerceIn(0, 100), pct < 0)
            .addAction(0, "Skip", actionPendingIntent(ACTION_SKIP))
            .addAction(0, "Cancel", actionPendingIntent(ACTION_CANCEL))

        try {
            builder.color = com.project.lol.webview.helpers.AccentTheme
                .resolveHex(this).toColorIntOrNull() ?: 0xFF1DB954.toInt()
        } catch (_: Exception) {
            builder.color = 0xFF1DB954.toInt()
        }
        return builder.build()
    }

    private fun actionPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).setAction(action)
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private fun String.toColorIntOrNull(): Int? = try {
    toColorInt()
} catch (_: IllegalArgumentException) {
    null
}