package com.qstarem.app

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

class QStaremApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) {
            Log.i(TAG, "Skipping GeckoRuntime init in child process: ${currentProcessName()}")
            return
        }
        if (GeckoRuntimeHolder.runtime == null) {
            val configPath = copyGeckoConfig()
            val settings = GeckoRuntimeSettings.Builder()
                .configFilePath(configPath)
                .javaScriptEnabled(true)
                .webFontsEnabled(true)
                .consoleOutput(true)
                .debugLogging(true)
                .fissionEnabled(false)
                .extensionsProcessEnabled(false)
                .isolatedProcessEnabled(false)
                .appZygoteProcessEnabled(false)
                .crashPullNeverShowAgain(true)
                .build()
            GeckoRuntimeHolder.runtime = GeckoRuntime.create(this, settings).also {
                Log.i(TAG, "GeckoRuntime created with config at $configPath")
            }
        }
    }

    private fun copyGeckoConfig(): String {
        val configFile = File(filesDir, "geckoview-config.yaml")
        assets.open("geckoview-config.yaml").use { input ->
            configFile.outputStream().use { output -> input.copyTo(output) }
        }
        return configFile.absolutePath
    }

    private fun isMainProcess(): Boolean = currentProcessName() == packageName

    private fun currentProcessName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        val pid = Process.myPid()
        val activityManager = getSystemService(ActivityManager::class.java)
        return activityManager.runningAppProcesses
            ?.firstOrNull { it.pid == pid }
            ?.processName
            ?: packageName
    }

    companion object {
        private const val TAG = "QStaremApplication"
    }
}

object GeckoRuntimeHolder {
    var runtime: GeckoRuntime? = null
}
