package com.qstarem.app.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val coordinator = QStaremMediaHolder.coordinator ?: return
        when (intent.action) {
            MediaSessionCoordinator.ACTION_MEDIA_TOGGLE -> {
                if (coordinator.isMediaPlaying.value) {
                    coordinator.pause()
                } else {
                    coordinator.play()
                }
            }
            MediaSessionCoordinator.ACTION_MEDIA_STOP -> {
                coordinator.stopPlayback()
            }
        }
    }
}

object QStaremMediaHolder {
    var coordinator: MediaSessionCoordinator? = null
}
