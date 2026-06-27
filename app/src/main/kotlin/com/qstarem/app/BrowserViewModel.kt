package com.qstarem.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qstarem.app.browser.BrowserSession
import com.qstarem.app.browser.ExtensionManager
import com.qstarem.app.browser.GeckoReadyWaiter
import android.util.Log
import com.qstarem.app.data.AppSettings
import com.qstarem.app.data.SettingsRepository
import com.qstarem.app.media.MediaSessionCoordinator
import com.qstarem.app.media.QStaremMediaHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppPhase {
    SPLASH,
    READY,
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val extensionManager = ExtensionManager()
    val mediaSessionCoordinator = MediaSessionCoordinator(application).also {
        QStaremMediaHolder.coordinator = it
    }

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings(),
    )

    private val _phase = MutableStateFlow(AppPhase.SPLASH)
    val phase: StateFlow<AppPhase> = _phase.asStateFlow()

    private val _splashMessage = MutableStateFlow("Installing extensions…")
    val splashMessage: StateFlow<String> = _splashMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadProgress = MutableStateFlow(0f)
    val loadProgress: StateFlow<Float> = _loadProgress.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _isInPictureInPicture = MutableStateFlow(false)
    val isInPictureInPicture: StateFlow<Boolean> = _isInPictureInPicture.asStateFlow()

    val isMediaPlaying: StateFlow<Boolean> = mediaSessionCoordinator.isMediaPlaying

    var onRequestPip: (() -> Boolean)? = null
    private var onEnterPipListener: (() -> Unit)? = null

    fun setPictureInPictureMode(inPip: Boolean) {
        _isInPictureInPicture.value = inPip
    }

    val browserSession: BrowserSession = BrowserSession(
        mediaSessionCoordinator = mediaSessionCoordinator,
        onProgress = { progress -> _loadProgress.value = progress },
        onLoadingChanged = { loading -> _isLoading.value = loading },
        onCanGoBackChanged = { canGoBack -> _canGoBack.value = canGoBack },
        onFullscreenChanged = { fullscreen -> _isFullscreen.value = fullscreen },
        onEnterPipRequested = { onEnterPipListener?.invoke() },
        onViewAttached = { viewAttachListener?.invoke() },
    )

    private var viewAttachListener: (() -> Unit)? = null
    private var geckoReady = false
    private var viewAttached = false
    private var startupComplete = false

    private var hasStarted = false

    fun configurePipBridge(onEnterPip: () -> Unit) {
        onEnterPipListener = onEnterPip
    }

    fun startIfNeeded() {
        if (hasStarted) return
        hasStarted = true

        viewModelScope.launch {
            val currentSettings = settings.value
            _splashMessage.value = "Starting browser engine…"

            viewAttachListener = {
                viewAttached = true
                maybeFinishStartup(currentSettings)
            }

            val ready = GeckoReadyWaiter.waitForRuntime()
            geckoReady = ready
            if (!ready) {
                Log.w(TAG, "Gecko not ready in time, continuing without guaranteed extension support")
            }
            maybeFinishStartup(currentSettings)
        }
    }

    private fun maybeFinishStartup(settings: AppSettings) {
        if (startupComplete || !geckoReady || !viewAttached) return
        startupComplete = true

        viewModelScope.launch {
            _splashMessage.value = "Loading Z-Stream…"
            browserSession.loadUrl(settings.homeUrl)
            _phase.value = AppPhase.READY

            Log.i(TAG, "Syncing bundled extensions (deferred)")
            extensionManager.syncExtensions(settings) { result ->
                if (result.isFailure) {
                    Log.e(TAG, "Extension sync failed", result.exceptionOrNull())
                } else {
                    Log.i(TAG, "Extensions synced successfully")
                }
            }
        }
    }

    fun reload() {
        browserSession.reload()
    }

    fun goBack() {
        browserSession.goBack()
    }

    fun goHome() {
        browserSession.loadUrl(settings.value.homeUrl)
    }

    fun goHomeOrPip() {
        if (isMediaPlaying.value) {
            onRequestPip?.invoke()
        } else {
            goHome()
        }
    }

    fun requestPipIfPlaying(): Boolean {
        return onRequestPip?.invoke() ?: false
    }

    fun applySettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.updateHomeUrl(newSettings.homeUrl)
            settingsRepository.updateAdBlocker(newSettings.adBlocker)
            settingsRepository.updatePStreamEnabled(newSettings.pStreamEnabled)

            _splashMessage.value = "Applying extension settings…"
            _phase.value = AppPhase.SPLASH
            extensionManager.syncExtensions(newSettings) {
                viewModelScope.launch {
                    browserSession.loadUrl(newSettings.homeUrl)
                    _phase.value = AppPhase.READY
                }
            }
        }
    }

    fun clearBrowsingData() {
        extensionManager.clearBrowsingData { }
    }

    override fun onCleared() {
        browserSession.release()
        QStaremMediaHolder.coordinator = null
        super.onCleared()
    }

    companion object {
        private const val TAG = "BrowserViewModel"
    }
}
