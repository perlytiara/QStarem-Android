package com.qstarem.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.UiThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.qstarem.app.MainActivity
import com.qstarem.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.MediaSession

class MediaSessionCoordinator(
    private val context: Context,
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _isMediaPlaying = MutableStateFlow(false)
    val isMediaPlaying: StateFlow<Boolean> = _isMediaPlaying.asStateFlow()

    private var activeMediaSession: MediaSession? = null
    private var currentTitle: String = "QStarem"
    private var currentArtist: String = "Z-Stream"
    private var artworkUri: String? = null

    val delegate = object : MediaSession.Delegate {
        @UiThread
        override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = mediaSession
            _isMediaPlaying.value = true
            publishNotification(isPlaying = true)
        }

        @UiThread
        override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
            if (activeMediaSession == mediaSession) {
                activeMediaSession = null
            }
            _isMediaPlaying.value = false
            cancelNotification()
        }

        @UiThread
        override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = mediaSession
            _isMediaPlaying.value = true
            publishNotification(isPlaying = true)
        }

        @UiThread
        override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = mediaSession
            _isMediaPlaying.value = false
            publishNotification(isPlaying = false)
        }

        @UiThread
        override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = null
            _isMediaPlaying.value = false
            cancelNotification()
        }
    }

    fun attachTo(session: GeckoSession) {
        ensureChannel()
        session.setMediaSessionDelegate(delegate)
    }

    fun updateMetadata(title: String?, artist: String?, artUri: String?) {
        currentTitle = title?.takeIf { it.isNotBlank() } ?: currentTitle
        currentArtist = artist?.takeIf { it.isNotBlank() } ?: currentArtist
        artworkUri = artUri ?: artworkUri
        if (_isMediaPlaying.value) {
            publishNotification(isPlaying = true)
        }
    }

    fun play() {
        activeMediaSession?.play()
    }

    fun pause() {
        activeMediaSession?.pause()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.media_playback_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.media_playback_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun publishNotification(isPlaying: Boolean) {
        ensureChannel()

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val toggleIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(ACTION_MEDIA_TOGGLE).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(ACTION_MEDIA_STOP).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setContentIntent(openIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) context.getString(R.string.media_pause) else context.getString(R.string.media_play),
                toggleIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.media_stop),
                stopIntent,
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1),
            )

        loadArtwork()?.let { bitmap ->
            builder.setLargeIcon(bitmap)
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun loadArtwork(): Bitmap? {
        val uri = artworkUri ?: return null
        return try {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun cancelNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "qstarem_media_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_MEDIA_TOGGLE = "com.qstarem.app.action.MEDIA_TOGGLE"
        const val ACTION_MEDIA_STOP = "com.qstarem.app.action.MEDIA_STOP"
    }
}
