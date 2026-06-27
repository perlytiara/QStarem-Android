package com.qstarem.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.qstarem.app.media.PipController
import com.qstarem.app.ui.BrowserScreen
import com.qstarem.app.ui.SettingsSheet
import com.qstarem.app.theme.QStaremTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()
    private lateinit var pipController: PipController

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        pipController = PipController(
            activity = this,
            isMediaPlayingProvider = { viewModel.isMediaPlaying.value },
        )

        viewModel.configurePipBridge {
            pipController.enterPipIfPlaying()
        }
        viewModel.onRequestPip = { pipController.enterPipIfPlaying() }

        requestNotificationPermissionIfNeeded()

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
                val isMediaPlaying by viewModel.isMediaPlaying.collectAsState()
                var showSettings by remember { mutableStateOf(false) }
                var controlsRevealed by remember { mutableStateOf(false) }

                viewModel.startIfNeeded()

                updateImmersiveMode(isMediaPlaying, controlsRevealed)

                BrowserScreen(
                    browserSession = viewModel.browserSession,
                    settings = settings,
                    isLoading = isLoading,
                    loadProgress = loadProgress,
                    canGoBack = canGoBack,
                    isFullscreen = isFullscreen,
                    isInPictureInPicture = isInPictureInPicture,
                    isMediaPlaying = isMediaPlaying,
                    controlsRevealed = controlsRevealed,
                    onControlsRevealedChange = { controlsRevealed = it },
                    showSplash = phase != AppPhase.READY,
                    splashMessage = splashMessage,
                    onBack = { viewModel.goBack() },
                    onReload = { viewModel.reload() },
                    onHome = { viewModel.goHomeOrPip() },
                    onEnterPip = { pipController.enterPipIfPlaying() },
                    onOpenSettings = { showSettings = true },
                    onSwipeUpForPip = { pipController.enterPipIfPlaying() },
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
        pipController.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setPictureInPictureMode(isInPictureInPictureMode)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateImmersiveMode(isMediaPlaying: Boolean, controlsRevealed: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (isMediaPlaying && !controlsRevealed) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
