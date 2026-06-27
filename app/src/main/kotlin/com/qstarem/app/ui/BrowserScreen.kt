package com.qstarem.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.qstarem.app.browser.BrowserSession
import com.qstarem.app.browser.GeckoBrowserView
import com.qstarem.app.data.AppSettings
import kotlinx.coroutines.delay

private const val CONTROLS_AUTO_HIDE_MS = 5_000L

@Composable
fun BrowserScreen(
    browserSession: BrowserSession,
    settings: AppSettings,
    isLoading: Boolean,
    loadProgress: Float,
    canGoBack: Boolean,
    isFullscreen: Boolean,
    isInPictureInPicture: Boolean,
    showSplash: Boolean,
    splashMessage: String,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    var controlsExpanded by remember { mutableStateOf(false) }
    val showChrome = !showSplash && !isFullscreen && !isInPictureInPicture

    LaunchedEffect(showChrome) {
        if (!showChrome) {
            controlsExpanded = false
        }
    }

    LaunchedEffect(controlsExpanded, showChrome) {
        if (controlsExpanded && showChrome) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsExpanded = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GeckoBrowserView(
            browserSession = browserSession,
            modifier = Modifier.fillMaxSize(),
        )

        if (isLoading && loadProgress < 1f && !isFullscreen) {
            LinearProgressIndicator(
                progress = { loadProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter),
                color = Color(0xFF9B5DE5),
                trackColor = Color(0x22000000),
            )
        }

        BrowserChromeOverlay(
            visible = showChrome,
            expanded = controlsExpanded,
            canGoBack = canGoBack,
            onExpand = { controlsExpanded = true },
            onBack = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onBack()
            },
            onReload = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onReload()
            },
            onHome = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onHome()
            },
            onEnterPip = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onEnterPip()
            },
            onOpenSettings = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onOpenSettings()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        )

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            SplashScreen(message = splashMessage)
        }
    }
}
