package com.qstarem.app.browser

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.qstarem.app.GeckoRuntimeHolder
import com.qstarem.app.data.AdBlockerChoice
import com.qstarem.app.data.AppSettings
import com.qstarem.app.media.MediaSessionCoordinator
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

class BrowserSession(
    private val mediaSessionCoordinator: MediaSessionCoordinator,
    private val onProgress: (Float) -> Unit,
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onCanGoBackChanged: (Boolean) -> Unit,
    private val onFullscreenChanged: (Boolean) -> Unit,
    private val onEnterPipRequested: () -> Unit,
    private val onSettingsSaveRequested: (AppSettings) -> Unit,
    private val onClearDataRequested: () -> Unit,
    private val onViewAttached: () -> Unit = {},
) {
    val session: GeckoSession = GeckoSession(
        GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(false)
            .build(),
    )

    private var isOpen = false
    private var isViewAttached = false
    private var pendingUrl: String? = null
    private var currentUrl: String? = null
    private var pendingSettingsPush: AppSettings? = null

    init {
        mediaSessionCoordinator.attachTo(session)

        session.progressDelegate = object : ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                onLoadingChanged(true)
                onProgress(0f)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                onLoadingChanged(false)
                onProgress(1f)
                pendingSettingsPush?.let { settings ->
                    pushSettingsToPage(settings)
                    pendingSettingsPush = null
                }
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                onProgress(progress / 100f)
            }

            override fun onSecurityChange(session: GeckoSession, securityInfo: ProgressDelegate.SecurityInformation) = Unit
        }

        session.navigationDelegate = object : NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                onCanGoBackChanged(canGoBack)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) = Unit

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                permissions: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean,
            ) {
                currentUrl = url
                if (url == null) return

                when {
                    url.contains(PIP_HASH) -> {
                        Log.i(TAG, "Bridge requested PiP via URL hash")
                        onEnterPipRequested()
                        restoreUrlWithoutHash(url, PIP_HASH)
                    }
                    url.contains(SAVE_SETTINGS_HASH) -> {
                        parseSettingsFromUrl(url)?.let { settings ->
                            Log.i(TAG, "Bridge saved app settings")
                            onSettingsSaveRequested(settings)
                        }
                        restoreUrlWithoutHash(url, SAVE_SETTINGS_HASH)
                    }
                    url.contains(CLEAR_DATA_HASH) -> {
                        Log.i(TAG, "Bridge requested clear browsing data")
                        onClearDataRequested()
                        restoreUrlWithoutHash(url, CLEAR_DATA_HASH)
                    }
                    url.contains(EXIT_PLAYER_HASH) -> Unit
                }
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                onFullscreenChanged(fullScreen)
            }
        }
    }

    fun attachToView(geckoView: GeckoView) {
        if (isViewAttached) return
        isViewAttached = true

        val runtime = requireNotNull(GeckoRuntimeHolder.runtime) { "GeckoRuntime not initialized" }
        Log.i(TAG, "Opening GeckoSession and attaching to GeckoView")
        session.open(runtime)
        isOpen = true
        geckoView.setSession(session)
        onViewAttached()

        pendingUrl?.let { url ->
            Log.i(TAG, "Loading queued URL: $url")
            session.loadUri(url)
            pendingUrl = null
        }
    }

    fun detachFromView(geckoView: GeckoView) {
        if (!isViewAttached) return
        geckoView.releaseSession()
        isViewAttached = false
    }

    fun loadUrl(url: String) {
        if (isOpen && isViewAttached) {
            Log.i(TAG, "Loading URL: $url")
            session.loadUri(url)
        } else {
            Log.i(TAG, "Queueing URL until view attached: $url")
            pendingUrl = url
        }
    }

    fun reload() {
        session.reload()
    }

    fun goBack(): Boolean {
        session.goBack()
        return true
    }

    fun requestExitPlayer() {
        val baseUrl = currentUrl?.substringBefore('#')?.takeIf { it.isNotBlank() }
        if (baseUrl != null) {
            Log.i(TAG, "Requesting player exit via bridge hash")
            session.loadUri("${baseUrl}$EXIT_PLAYER_HASH")
        } else {
            goBack()
        }
    }

    fun queueSettingsPush(settings: AppSettings) {
        pendingSettingsPush = settings
        if (isOpen && isViewAttached) {
            pushSettingsToPage(settings)
            pendingSettingsPush = null
        }
    }

    fun pushSettingsToPage(settings: AppSettings) {
        if (!isOpen || !isViewAttached) return
        val json =
            """{"homeUrl":"${settings.homeUrl.replace("\"", "\\\"")}","adBlocker":"${settings.adBlocker.name}","pStreamEnabled":${settings.pStreamEnabled},"appIconId":${settings.appIconId}}"""
        val encoded = Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
        val script =
            "javascript:(function(){try{localStorage.setItem('qstarem-app-settings',atob('$encoded'));window.dispatchEvent(new Event('qstarem-settings-updated'));}catch(e){}})()"
        session.loadUri(script)
    }

    fun release() {
        session.close()
        isOpen = false
        isViewAttached = false
    }

    private fun restoreUrlWithoutHash(url: String, hashMarker: String) {
        val cleanUrl = url.substringBefore(hashMarker).trimEnd('#', '?', '&')
        if (cleanUrl.isNotBlank() && cleanUrl != url) {
            session.loadUri(cleanUrl)
        }
    }

    private fun parseSettingsFromUrl(url: String): AppSettings? {
        val fragment = url.substringAfter('#', "")
        val query = fragment.substringAfter('?', "")
        if (query.isBlank()) return null

        val params = query.split('&').mapNotNull { part ->
            val pieces = part.split('=', limit = 2)
            if (pieces.size == 2) pieces[0] to Uri.decode(pieces[1]) else null
        }.toMap()

        val homeUrl = params["home"]?.takeIf { it.isNotBlank() } ?: AppSettings.DEFAULT_HOME_URL
        val adBlocker = when (params["blocker"]?.lowercase()) {
            "adguard" -> AdBlockerChoice.ADGUARD
            "none" -> AdBlockerChoice.NONE
            else -> AdBlockerChoice.UBLOCK
        }
        val pStreamEnabled = params["pstream"] != "0"
        val appIconId = params["icon"]?.toIntOrNull()?.coerceIn(
            AppSettings.MIN_APP_ICON_ID,
            AppSettings.MAX_APP_ICON_ID,
        ) ?: AppSettings.DEFAULT_APP_ICON_ID

        return AppSettings(
            homeUrl = homeUrl,
            adBlocker = adBlocker,
            pStreamEnabled = pStreamEnabled,
            appIconId = appIconId,
        )
    }

    companion object {
        private const val TAG = "BrowserSession"
        private const val EXIT_PLAYER_HASH = "#qstarem-exit"
        private const val PIP_HASH = "#qstarem-pip"
        private const val SAVE_SETTINGS_HASH = "#qstarem-save"
        private const val CLEAR_DATA_HASH = "#qstarem-clear"
    }
}
