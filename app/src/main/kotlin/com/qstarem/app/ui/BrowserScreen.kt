package com.qstarem.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.qstarem.app.browser.BrowserSession
import com.qstarem.app.browser.GeckoBrowserView
import com.qstarem.app.update.UpdateUiState

private val BOTTOM_GESTURE_HEIGHT = 48.dp

@Composable
fun BrowserScreen(
    browserSession: BrowserSession,
    isLoading: Boolean,
    loadProgress: Float,
    isFullscreen: Boolean,
    isMediaPlaying: Boolean,
    showSplash: Boolean,
    splashMessage: String,
    onSwipeUpForPip: () -> Unit,
    updateState: UpdateUiState,
    onConfirmUpdateDownload: () -> Unit,
    onDismissUpdateDownload: () -> Unit,
    onInstallUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bottomGesturePx = with(density) { BOTTOM_GESTURE_HEIGHT.toPx() }
    var bottomDragAccumulation by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        GeckoBrowserView(
            browserSession = browserSession,
            modifier = Modifier.fillMaxSize(),
        )

        if (isMediaPlaying && !isFullscreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(BOTTOM_GESTURE_HEIGHT)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { bottomDragAccumulation = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                bottomDragAccumulation += dragAmount
                            },
                            onDragEnd = {
                                if (bottomDragAccumulation <= -bottomGesturePx * 0.6f) {
                                    onSwipeUpForPip()
                                }
                                bottomDragAccumulation = 0f
                            },
                        )
                    },
            )
        }

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

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            SplashScreen(message = splashMessage)
        }

        if (!isFullscreen && !showSplash) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                UpdateBanner(
                    state = updateState,
                    onConfirmDownload = onConfirmUpdateDownload,
                    onDismissDownload = onDismissUpdateDownload,
                    onInstallUpdate = onInstallUpdate,
                )
            }
        }
    }
}
