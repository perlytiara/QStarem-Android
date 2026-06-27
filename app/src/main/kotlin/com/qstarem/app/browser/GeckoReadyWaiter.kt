package com.qstarem.app.browser

import android.util.Log
import com.qstarem.app.GeckoRuntimeHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mozilla.geckoview.GeckoResult
import kotlin.coroutines.resume

object GeckoReadyWaiter {
    suspend fun waitForRuntime(timeoutMs: Long = 20_000): Boolean {
        val runtime = GeckoRuntimeHolder.runtime ?: return false
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (awaitResult(runtime.webExtensionController.list()) != null) {
                Log.i(TAG, "Gecko runtime is ready")
                return true
            }
            delay(250)
        }
        Log.w(TAG, "Timed out waiting for Gecko runtime")
        return false
    }

    private suspend fun <T> awaitResult(result: GeckoResult<T>): T? {
        return suspendCancellableCoroutine { continuation ->
            result.accept(
                { value -> if (continuation.isActive) continuation.resume(value) },
                { _ -> if (continuation.isActive) continuation.resume(null) },
            )
        }
    }

    private const val TAG = "GeckoReadyWaiter"
}
