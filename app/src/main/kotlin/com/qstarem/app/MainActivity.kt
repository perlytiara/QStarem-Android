package com.qstarem.app

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.qstarem.app.ui.BrowserScreen
import com.qstarem.app.ui.SettingsSheet
import com.qstarem.app.theme.QStaremTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()
    private var pipParams: PictureInPictureParams? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setAutoEnterEnabled(true)
                .build()
            pipParams = params
            setPictureInPictureParams(params)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.canGoBack.value) {
                viewModel.goBack()
            } else {
                finish()
            }
        }

        setContent {
            QStaremTheme {
                val settings by viewModel.settings.collectAsState()
                val phase by viewModel.phase.collectAsState()
                val splashMessage by viewModel.splashMessage.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val loadProgress by viewModel.loadProgress.collectAsState()
                val canGoBack by viewModel.canGoBack.collectAsState()
                val isFullscreen by viewModel.isFullscreen.collectAsState()
                val isInPictureInPicture by viewModel.isInPictureInPicture.collectAsState()
                var showSettings by remember { mutableStateOf(false) }

                viewModel.startIfNeeded()

                BrowserScreen(
                    browserSession = viewModel.browserSession,
                    settings = settings,
                    isLoading = isLoading,
                    loadProgress = loadProgress,
                    canGoBack = canGoBack,
                    isFullscreen = isFullscreen,
                    isInPictureInPicture = isInPictureInPicture,
                    showSplash = phase != AppPhase.READY,
                    splashMessage = splashMessage,
                    onBack = { viewModel.goBack() },
                    onReload = { viewModel.reload() },
                    onHome = { viewModel.goHome() },
                    onEnterPip = { enterPipMode() },
                    onOpenSettings = { showSettings = true },
                )

                if (showSettings) {
                    SettingsSheet(
                        settings = settings,
                        onDismiss = { showSettings = false },
                        onSave = { updated ->
                            viewModel.applySettings(updated)
                            showSettings = false
                        },
                        onClearData = { viewModel.clearBrowsingData() },
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (supportsPip()) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setPictureInPictureMode(isInPictureInPictureMode)
    }

    private fun enterPipMode() {
        if (!supportsPip()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = pipParams ?: PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    private fun supportsPip(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
}
