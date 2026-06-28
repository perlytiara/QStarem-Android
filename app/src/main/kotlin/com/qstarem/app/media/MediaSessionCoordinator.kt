package com.qstarem.app.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.UiThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
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

    private val mediaSession = MediaSessionCompat(context, "QStarem").apply {
        setCallback(
            object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    activeMediaSession?.play()
                }

                override fun onPause() {
                    activeMediaSession?.pause()
                }

                override fun onStop() {
                    stopPlayback()
                }
            },
        )
        isActive = true
    }

    private val _isMediaPlaying = MutableStateFlow(false)
    val isMediaPlaying: StateFlow<Boolean> = _isMediaPlaying.asStateFlow()

    private var activeMediaSession: MediaSession? = null
    private var currentTitle: String = "QStarem"
    private var currentArtist: String = "Z-Stream"
    private var artworkBitmap: Bitmap? = null
    private var sessionActive = false

    val delegate = object : MediaSession.Delegate {
        @UiThread
        override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = mediaSession
            sessionActive = true
            _isMediaPlaying.value = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            publishPlaybackNotification(isPlaying = true)
        }

        @UiThread
        override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
            if (activeMediaSession == mediaSession) {
                activeMediaSession = null
            }
            sessionActive = false
            _isMediaPlaying.value = false
            artworkBitmap = null
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            clearPlaybackNotification()
        }

        @UiThread
        override fun onMetadata(
            session: GeckoSession,
            mediaSession: MediaSession,
            metadata: MediaSession.Metadata,
        ) {
            currentTitle = metadata.title?.takeIf { it.isNotBlank() } ?: currentTitle
            currentArtist = metadata.artist?.takeIf { it.isNotBlank() } ?: currentArtist

            val artwork = metadata.artwork
            if (artwork != null) {
                artwork.getBitmap(ARTWORK_SIZE).accept(
                    { bitmap ->
                        artworkBitmap = bitmap
                        if (sessionActive) {
                            publishPlaybackNotification(_isMediaPlaying.value)
                        }
                    },
                    {
                        if (sessionActive) {
                            publishPlaybackNotification(_isMediaPlaying.value)
                        }
                    },
                )
            } else if (sessionActive) {
                publishPlaybackNotification(_isMediaPlaying.value)
            }
        }

        @UiThread
        override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = mediaSession
            sessionActive = true
            _isMediaPlaying.value = true
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            publishPlaybackNotification(isPlaying = true)
        }

        @UiThread
        override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = mediaSession
            _isMediaPlaying.value = false
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            publishPlaybackNotification(isPlaying = false)
        }

        @UiThread
        override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
            activeMediaSession = null
            sessionActive = false
            _isMediaPlaying.value = false
            artworkBitmap = null
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            clearPlaybackNotification()
        }
    }

    fun attachTo(session: GeckoSession) {
        ensureChannel()
        session.setMediaSessionDelegate(delegate)
    }

    fun play() {
        activeMediaSession?.play()
    }

    fun pause() {
        activeMediaSession?.pause()
    }

    fun stopPlayback() {
        activeMediaSession?.stop()
        activeMediaSession = null
        sessionActive = false
        _isMediaPlaying.value = false
        artworkBitmap = null
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        clearPlaybackNotification()
    }

    fun buildNotification(): Notification? {
        if (!sessionActive) return null
        return createNotificationBuilder(_isMediaPlaying.value).build()
    }

    private fun publishPlaybackNotification(isPlaying: Boolean) {
        ensureChannel()
        updatePlaybackState(
            if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
        )
        MediaPlaybackService.start(context)
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            createNotificationBuilder(isPlaying).build(),
        )
    }

    private fun clearPlaybackNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        MediaPlaybackService.stop(context)
        mediaSession.isActive = false
        mediaSession.isActive = true
    }

    private fun createNotificationBuilder(isPlaying: Boolean): NotificationCompat.Builder {
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
            .setSmallIcon(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSubText(context.getString(R.string.app_name))
            .setContentIntent(openIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
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
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )

        artworkBitmap?.let { bitmap ->
            builder.setLargeIcon(bitmap)
        }

        return builder
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP,
            )
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
            .build()
        mediaSession.setPlaybackState(playbackState)
        mediaSession.setMetadata(
            android.support.v4.media.MediaMetadataCompat.Builder()
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                .putString(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST, currentArtist)
                .putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artworkBitmap)
                .putBitmap(android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, artworkBitmap)
                .build(),
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.media_playback_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.media_playback_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "qstarem_media_playback"
        const val NOTIFICATION_ID = 1001
        const val ACTION_MEDIA_TOGGLE = "com.qstarem.app.action.MEDIA_TOGGLE"
        const val ACTION_MEDIA_STOP = "com.qstarem.app.action.MEDIA_STOP"
        private const val ARTWORK_SIZE = 512
    }
}
