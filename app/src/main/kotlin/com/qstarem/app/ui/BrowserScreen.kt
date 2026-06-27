package com.qstarem.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.qstarem.app.browser.BrowserSession
import com.qstarem.app.browser.GeckoBrowserView
import com.qstarem.app.data.AppSettings
import kotlinx.coroutines.delay

private const val CONTROLS_AUTO_HIDE_MS = 5_000L
private val TOP_GESTURE_HEIGHT = 48.dp
private val BOTTOM_GESTURE_HEIGHT = 48.dp

@Composable
fun BrowserScreen(
    browserSession: BrowserSession,
    settings: AppSettings,
    isLoading: Boolean,
    loadProgress: Float,
    canGoBack: Boolean,
    isFullscreen: Boolean,
    isInPictureInPicture: Boolean,
    isMediaPlaying: Boolean,
    controlsRevealed: Boolean,
    onControlsRevealedChange: (Boolean) -> Unit,
    showSplash: Boolean,
    splashMessage: String,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeUpForPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val topGesturePx = with(density) { TOP_GESTURE_HEIGHT.toPx() }
    val bottomGesturePx = with(density) { BOTTOM_GESTURE_HEIGHT.toPx() }
    var controlsExpanded by remember { mutableStateOf(false) }
    var dragAccumulation by remember { mutableFloatStateOf(0f) }

    val showChrome = !showSplash && !isFullscreen && !isInPictureInPicture &&
        (!isMediaPlaying || controlsRevealed)

    LaunchedEffect(isMediaPlaying) {
        if (!isMediaPlaying) {
            onControlsRevealedChange(false)
            controlsExpanded = false
        }
    }

    LaunchedEffect(showChrome, controlsRevealed) {
        if (!showChrome) {
            controlsExpanded = false
        } else if (controlsRevealed) {
            controlsExpanded = true
        }
    }

    LaunchedEffect(controlsExpanded, showChrome) {
        if (controlsExpanded && showChrome) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsExpanded = false
            onControlsRevealedChange(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isMediaPlaying, showChrome) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulation = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumulation += dragAmount
                    },
                    onDragEnd = {
                        if (isMediaPlaying && dragAccumulation <= -bottomGesturePx * 0.6f) {
                            onSwipeUpForPip()
                        } else if (dragAccumulation >= topGesturePx * 0.6f) {
                            onControlsRevealedChange(true)
                            controlsExpanded = true
                        }
                        dragAccumulation = 0f
                    },
                )
            },
    ) {
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
            showCollapsedHint = !isMediaPlaying && !controlsExpanded,
            canGoBack = canGoBack,
            onExpand = {
                onControlsRevealedChange(true)
                controlsExpanded = true
            },
            onBack = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onControlsRevealedChange(false)
                onBack()
            },
            onReload = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onControlsRevealedChange(false)
                onReload()
            },
            onHome = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onControlsRevealedChange(false)
                onHome()
            },
            onEnterPip = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onControlsRevealedChange(false)
                onEnterPip()
            },
            onOpenSettings = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                controlsExpanded = false
                onControlsRevealedChange(false)
                onOpenSettings()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp),
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
