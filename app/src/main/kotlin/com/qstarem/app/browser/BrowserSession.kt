package com.qstarem.app.browser

import android.util.Log
import com.qstarem.app.GeckoRuntimeHolder
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.NavigationDelegate
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

class BrowserSession(
    private val onProgress: (Float) -> Unit,
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onCanGoBackChanged: (Boolean) -> Unit,
    private val onFullscreenChanged: (Boolean) -> Unit,
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

    init {
        session.progressDelegate = object : ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                onLoadingChanged(true)
                onProgress(0f)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                onLoadingChanged(false)
                onProgress(1f)
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
            ) = Unit
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

    fun release() {
        session.close()
        isOpen = false
        isViewAttached = false
    }

    companion object {
        private const val TAG = "BrowserSession"
    }
}
