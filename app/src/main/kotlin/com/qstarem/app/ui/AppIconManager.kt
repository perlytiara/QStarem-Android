package com.qstarem.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {
    private val iconIds = 1..6

    fun apply(context: Context, iconId: Int) {
        val selected = iconId.coerceIn(1, 6)
        val packageManager = context.packageManager
        val packageName = context.packageName

        iconIds.forEach { id ->
            val component = ComponentName(packageName, "$packageName.MainActivityIcon$id")
            val state = if (id == selected) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                packageManager.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP,
                )
            } catch (error: IllegalArgumentException) {
                Log.e(TAG, "Failed to toggle icon alias $id", error)
            }
        }
    }

    private const val TAG = "AppIconManager"
}
