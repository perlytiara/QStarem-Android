package com.qstarem.app.media

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.widget.Toast

class PipController(
    private val activity: Activity,
    private var isMediaPlayingProvider: () -> Boolean,
    private var aspectRatioProvider: () -> Rational = { Rational(16, 9) },
) {
    private var pipParams: PictureInPictureParams? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setAutoEnterEnabled(true)
                .build()
            activity.setPictureInPictureParams(pipParams!!)
        }
    }

    fun updateMediaState(isPlaying: Boolean, aspectRatio: Rational? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val ratio = aspectRatio ?: aspectRatioProvider()
            pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(ratio)
                .setAutoEnterEnabled(isPlaying)
                .build()
            activity.setPictureInPictureParams(pipParams!!)
        }
    }

    fun enterPipIfPlaying(): Boolean {
        if (!supportsPip()) {
            Toast.makeText(activity, "Picture-in-Picture is not supported on this device", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!isMediaPlayingProvider()) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ratio = aspectRatioProvider()
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(ratio)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setAutoEnterEnabled(true)
                    }
                }
                .build()
            activity.enterPictureInPictureMode(params)
            return true
        }

        return false
    }

    fun onUserLeaveHint() {
        if (isMediaPlayingProvider()) {
            enterPipIfPlaying()
        }
    }

    private fun supportsPip(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
}
