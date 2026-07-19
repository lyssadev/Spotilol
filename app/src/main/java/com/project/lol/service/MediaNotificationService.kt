package com.project.lol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import com.project.lol.R
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

class MediaNotificationService : Service() {

    companion object {
        private const val TAG = "MediaNotifService"
        private const val CHANNEL_ID = "spotilol_media_playback"
        private const val NOTIFICATION_ID = 1

        private const val ACTION_PLAY_PAUSE = "com.project.lol.ACTION_PLAY_PAUSE"
        private const val ACTION_NEXT = "com.project.lol.ACTION_NEXT"
        private const val ACTION_PREV = "com.project.lol.ACTION_PREV"
        private const val ACTION_FAVORITE = "com.project.lol.ACTION_FAVORITE"

        private const val CUSTOM_ACTION_TOGGLE_FAV = "toggle_fav"

        private val PLAYBACK_ACTIONS: Long =
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SEEK_TO

        private const val NOTIF_COLOR = 0xFFE0E0E0.toInt()

        var webView: WebView? = null
        var instance: MediaNotificationService? = null
    }

    private lateinit var mediaSession: MediaSessionCompat
    private var isPlaying = false
    private var isFavorite = false
    private var coverBitmap: Bitmap? = null
    private var currentTitle = ""
    private var currentArtist = ""
    private var currentPosition: Long = 0L
    private var currentDuration: Long = 0L
    private var lastCoverUrl = ""
    private var wakeLock: PowerManager.WakeLock? = null

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PLAY_PAUSE -> {
                    webView?.evaluateJavascript("actPlayPause(${!isPlaying})", null)
                }
                ACTION_NEXT -> webView?.evaluateJavascript("actSkipForward()", null)
                ACTION_PREV -> webView?.evaluateJavascript("actSkipBack()", null)
                ACTION_FAVORITE -> webView?.evaluateJavascript("actAddToFav()", null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        setupMediaSession()
        registerReceivers()
        try {
            startForeground(NOTIFICATION_ID, buildNotification(), getStartForegroundServiceType())
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to start foreground", e)
            stopSelf()
            return
        }
    }

    @Suppress("DEPRECATION")
    private fun getStartForegroundServiceType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        instance = null
        try { unregisterReceiver(actionReceiver) } catch (_: Exception) {}
        mediaSession.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Spotilol media playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "SpotilolSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    webView?.evaluateJavascript("actPlayPause(true)", null)
                }

                override fun onPause() {
                    webView?.evaluateJavascript("actPlayPause(false)", null)
                }

                override fun onSkipToNext() {
                    webView?.evaluateJavascript("actSkipForward()", null)
                }

                override fun onSkipToPrevious() {
                    webView?.evaluateJavascript("actSkipBack()", null)
                }

                override fun onStop() {
                    webView?.evaluateJavascript("actPlayPause(false)", null)
                }

                override fun onSeekTo(pos: Long) {
                    webView?.evaluateJavascript("actSeek($pos)", null)
                }

                override fun onCustomAction(action: String?, extras: Bundle?) {
                    if (action == CUSTOM_ACTION_TOGGLE_FAV) {
                        webView?.evaluateJavascript("actAddToFav()", null)
                    }
                }
            })
            isActive = true
        }
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(ACTION_PLAY_PAUSE)
            addAction(ACTION_NEXT)
            addAction(ACTION_PREV)
            addAction(ACTION_FAVORITE)
            addAction(Intent.ACTION_MEDIA_BUTTON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }
    }

    fun updateFromMediaStatus(json: String) {
        try {
            val obj = org.json.JSONObject(json)
            currentTitle = obj.optString("track", "")
            currentArtist = obj.optString("artist", "")
            isPlaying = obj.optBoolean("playing", false)
            isFavorite = obj.optBoolean("fav", false)
            currentDuration = obj.optLong("duration", 0L)
            currentPosition = obj.optLong("position", 0L)
            val coverUrl = obj.optString("cover", "")

            if (isPlaying) acquireWakeLock() else releaseWakeLock()

            if (coverUrl.isNotEmpty() && coverUrl != "null" && coverUrl != lastCoverUrl) {
                lastCoverUrl = coverUrl
                loadCoverArt(coverUrl)
            } else if (coverUrl.isEmpty() || coverUrl == "null") {
                lastCoverUrl = ""
                coverBitmap = null
            }

            updatePlaybackState()
            updateMetadata()
            showNotification()
        } catch (_: Exception) {}
    }

    fun updatePlaybackPosition(position: Long) {
        currentPosition = position
        updatePlaybackState()
    }

    private fun updatePlaybackState() {
        val favIcon = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
        val state = PlaybackStateCompat.Builder()
            .setActions(PLAYBACK_ACTIONS)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING
                else PlaybackStateCompat.STATE_PAUSED,
                currentPosition, if (isPlaying) 1f else 0f
            )
            .addCustomAction(
                CUSTOM_ACTION_TOGGLE_FAV,
                if (isFavorite) "Unlike" else "Like",
                favIcon
            )
            .build()
        mediaSession.setPlaybackState(state)
    }

    private fun updateMetadata() {
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Spotilol")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentDuration)
        coverBitmap?.let { bmp ->
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bmp)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bmp)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bmp)
        }
        mediaSession.setMetadata(builder.build())
    }

    private fun loadCoverArt(url: String) {
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()
                val stream = conn.inputStream
                val raw = BitmapFactory.decodeStream(stream)
                stream.close()
                conn.disconnect()
                if (raw != null) {
                    val target = 512
                    val scale = min(target.toFloat() / raw.width, target.toFloat() / raw.height)
                    val w = (raw.width * scale).toInt()
                    val h = (raw.height * scale).toInt()
                    val scaled = Bitmap.createScaledBitmap(raw, w, h, true)
                    if (scaled != raw) raw.recycle()
                    coverBitmap = scaled
                    updateMetadata()
                    showNotification()
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun showNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_prev, "Previous", getActionPendingIntent(ACTION_PREV)
        ).build()

        val playPauseAction = NotificationCompat.Action.Builder(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            if (isPlaying) "Pause" else "Play",
            getActionPendingIntent(ACTION_PLAY_PAUSE)
        ).build()

        val nextAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_next, "Next", getActionPendingIntent(ACTION_NEXT)
        ).build()

        val favAction = NotificationCompat.Action.Builder(
            if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite,
            if (isFavorite) "Unlike" else "Like",
            getActionPendingIntent(ACTION_FAVORITE)
        ).build()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle.ifEmpty { "Spotilol" })
            .setContentText(currentArtist)
            .setSubText("Spotilol")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(NOTIF_COLOR)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(favAction)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(getActionPendingIntent(ACTION_PLAY_PAUSE))
            )

        coverBitmap?.let { builder.setLargeIcon(it) }

        return builder.build()
    }

    private fun getActionPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(packageName)
        return PendingIntent.getBroadcast(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "spotilol:media_playback"
            ).apply { acquire(60 * 60 * 1000L) }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
            wakeLock = null
        }
    }
}
