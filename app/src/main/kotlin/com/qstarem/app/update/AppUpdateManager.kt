package com.qstarem.app.update

import android.app.Activity
import android.content.Context
import com.qstarem.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

enum class UpdatePhase {
    Idle,
    Checking,
    AwaitingDownloadConsent,
    Downloading,
    Ready,
    Error,
}

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.Idle,
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val availableVersion: String? = null,
    val notes: String? = null,
    val progress: Float = 0f,
    val message: String? = null,
)

class AppUpdateManager(context: Context) {
    private val appContext = context.applicationContext
    private val checker = UpdateChecker()
    private val downloader = ApkDownloader(appContext)
    private val installer = ApkInstaller()

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var pendingManifest: UpdateManifest? = null
    private var downloadedApk: File? = null

    suspend fun checkForUpdates(manual: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(
                    phase = UpdatePhase.Checking,
                    message = if (manual) "Checking for updates…" else "Looking for updates…",
                )

                val manifest = checker.fetchLatestManifest()
                    ?: run {
                        _state.value = _state.value.copy(
                            phase = UpdatePhase.Error,
                            message = "Could not read update manifest.",
                        )
                        return@withContext
                    }

                if (manifest.versionCode <= BuildConfig.VERSION_CODE) {
                    pendingManifest = null
                    downloadedApk = null
                    _state.value = _state.value.copy(
                        phase = UpdatePhase.Idle,
                        availableVersion = manifest.version,
                        notes = null,
                        progress = 0f,
                        message = "You're up to date. Installed v${BuildConfig.VERSION_NAME}, latest v${manifest.version}.",
                    )
                    return@withContext
                }

                pendingManifest = manifest
                downloadPending(manifest, background = !manual)
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    phase = UpdatePhase.Error,
                    message = error.message ?: "Update check failed.",
                )
            }
        }
    }

    suspend fun confirmMobileDownload() {
        val manifest = pendingManifest ?: return
        downloadPending(manifest, background = false)
    }

    fun dismissPendingDownload() {
        _state.value = _state.value.copy(
            phase = UpdatePhase.Idle,
            message = "Update download postponed.",
        )
    }

    fun dismissReadyInstall() {
        _state.value = _state.value.copy(
            phase = UpdatePhase.Idle,
            message = "Update ready — install from App settings when you want.",
        )
    }

    fun installReadyUpdate(activity: Activity) {
        val apk = downloadedApk
        if (apk == null || !apk.isFile) {
            _state.value = _state.value.copy(
                phase = UpdatePhase.Error,
                message = "No downloaded update is ready to install.",
            )
            return
        }
        installer.install(activity, apk)
    }

    private suspend fun downloadPending(manifest: UpdateManifest, background: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(
                    phase = UpdatePhase.Downloading,
                    availableVersion = manifest.version,
                    notes = manifest.notes,
                    progress = 0f,
                    message = if (background) {
                        "Downloading update in background…"
                    } else {
                        "Downloading QStarem ${manifest.version}…"
                    },
                )

                val apk = downloader.download(manifest) { progress ->
                    _state.value = _state.value.copy(
                        phase = UpdatePhase.Downloading,
                        progress = progress,
                        message = if (background) {
                            "Downloading update in background…"
                        } else {
                            "Downloading QStarem ${manifest.version}…"
                        },
                    )
                }

                downloadedApk = apk
                _state.value = _state.value.copy(
                    phase = UpdatePhase.Ready,
                    progress = 1f,
                    message = "QStarem ${manifest.version} is ready to install.",
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    phase = UpdatePhase.Error,
                    message = error.message ?: "Update download failed.",
                )
            }
        }
    }
}
