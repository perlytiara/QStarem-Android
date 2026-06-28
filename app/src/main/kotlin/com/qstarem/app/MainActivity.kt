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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.qstarem.app.media.PipController
import com.qstarem.app.ui.AppIconManager
import com.qstarem.app.ui.BrowserScreen
import com.qstarem.app.ui.UpdateReadyDialog
import com.qstarem.app.update.UpdatePhase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.qstarem.app.theme.QStaremTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()
    private lateinit var pipController: PipController

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)

        pipController = PipController(
            activity = this,
            isMediaPlayingProvider = { viewModel.isMediaPlaying.value },
        )

        viewModel.configurePipBridge {
            pipController.enterPipIfPlaying()
        }
        viewModel.onRequestPip = { pipController.enterPipIfPlaying() }
        viewModel.onInstallUpdateRequested = {
            viewModel.installReadyUpdate(this@MainActivity)
        }

        requestNotificationPermissionIfNeeded()

        lifecycleScope.launch {
            AppIconManager.apply(this@MainActivity, viewModel.settings.first().appIconId)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.isMediaPlaying.value) {
                viewModel.exitPlayerOrBack()
            } else if (viewModel.canGoBack.value) {
                viewModel.goBack()
            } else {
                finish()
            }
        }

        setContent {
            QStaremTheme {
                val phase by viewModel.phase.collectAsState()
                val splashMessage by viewModel.splashMessage.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val loadProgress by viewModel.loadProgress.collectAsState()
                val isFullscreen by viewModel.isFullscreen.collectAsState()
                val isInPictureInPicture by viewModel.isInPictureInPicture.collectAsState()
                val isMediaPlaying by viewModel.isMediaPlaying.collectAsState()
                val updateState by viewModel.updateState.collectAsState()
                var showUpdateDialog by remember { mutableStateOf(false) }

                LaunchedEffect(updateState.phase) {
                    if (updateState.phase == UpdatePhase.Ready) {
                        showUpdateDialog = true
                    }
                }

                viewModel.startIfNeeded()

                BrowserScreen(
                    browserSession = viewModel.browserSession,
                    isLoading = isLoading,
                    loadProgress = loadProgress,
                    isFullscreen = isFullscreen || isInPictureInPicture,
                    isMediaPlaying = isMediaPlaying,
                    showSplash = phase != AppPhase.READY,
                    splashMessage = splashMessage,
                    onSwipeUpForPip = { pipController.enterPipIfPlaying() },
                )

                UpdateReadyDialog(
                    state = updateState,
                    visible = showUpdateDialog,
                    onInstall = {
                        showUpdateDialog = false
                        viewModel.installReadyUpdate(this@MainActivity)
                    },
                    onDismiss = {
                        showUpdateDialog = false
                        viewModel.dismissReadyUpdate()
                    },
                )
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
}
