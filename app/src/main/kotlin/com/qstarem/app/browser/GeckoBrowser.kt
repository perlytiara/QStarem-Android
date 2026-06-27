package com.qstarem.app.browser

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoView

@Composable
fun GeckoBrowserView(
    browserSession: BrowserSession,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val geckoView = remember {
        GeckoView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setViewBackend(GeckoView.BACKEND_TEXTURE_VIEW)
        }
    }

    DisposableEffect(browserSession) {
        browserSession.attachToView(geckoView)
        onDispose {
            browserSession.detachFromView(geckoView)
        }
    }

    AndroidView(
        factory = { geckoView },
        modifier = modifier,
    )
}
