package com.qstarem.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qstarem.app.R

private val ChromeGlass = Color(0xB30A0A12)
private val ChromeBorder = Color(0x339B5DE5)
private val ChromeAccent = Color(0xFF9B5DE5)
private val ChromeIcon = Color(0xFFECECF4)
private val ChromeIconMuted = Color(0x66ECECF4)
private val ChromeLabel = Color(0x99ECECF4)

@Composable
fun BrowserChromeOverlay(
    visible: Boolean,
    expanded: Boolean,
    canGoBack: Boolean,
    onExpand: () -> Unit,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onEnterPip: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = tween(180)) + fadeOut(),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ChromeGlass,
                border = BorderStroke(1.dp, ChromeBorder),
                shadowElevation = 12.dp,
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChromeAction(
                        enabled = canGoBack,
                        onClick = onBack,
                        contentDescription = stringResource(R.string.back),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = if (canGoBack) ChromeIcon else ChromeIconMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    ChromeAction(onClick = onReload, contentDescription = stringResource(R.string.reload)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = ChromeIcon, modifier = Modifier.size(22.dp))
                    }
                    ChromeAction(onClick = onHome, contentDescription = stringResource(R.string.home)) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = ChromeAccent, modifier = Modifier.size(22.dp))
                    }
                    ChromeAction(onClick = onEnterPip, contentDescription = stringResource(R.string.picture_in_picture)) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = ChromeIcon, modifier = Modifier.size(22.dp))
                    }
                    ChromeAction(onClick = onOpenSettings, contentDescription = stringResource(R.string.settings)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ChromeIcon, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(120)),
        ) {
            Surface(
                onClick = onExpand,
                shape = RoundedCornerShape(20.dp),
                color = ChromeGlass,
                border = BorderStroke(1.dp, ChromeBorder),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = ChromeAccent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.tap_for_controls),
                        color = ChromeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChromeAction(
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
    ) {
        content()
    }
}
