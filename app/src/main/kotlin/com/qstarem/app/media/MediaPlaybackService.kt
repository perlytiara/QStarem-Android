package com.qstarem.app.media

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

class MediaPlaybackService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val coordinator = QStaremMediaHolder.coordinator
        if (coordinator == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_STOP -> {
                coordinator.stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = coordinator.buildNotification() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(MediaSessionCoordinator.NOTIFICATION_ID, notification)
        return START_STICKY
    }

    companion object {
        const val ACTION_STOP = "com.qstarem.app.action.STOP_PLAYBACK_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MediaPlaybackService::class.java))
        }
    }
}
