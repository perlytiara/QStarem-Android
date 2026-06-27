package com.qstarem.app.browser

import android.util.Log
import com.qstarem.app.GeckoRuntimeHolder
import com.qstarem.app.data.AdBlockerChoice
import com.qstarem.app.data.AppSettings
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

class BundledExtensionPromptDelegate : WebExtensionController.PromptDelegate {
    override fun onInstallPromptRequest(
        extension: WebExtension,
        permissions: Array<out String>,
        origins: Array<out String>,
        optionalPermissions: Array<out String>,
    ): GeckoResult<WebExtension.PermissionPromptResponse?> {
        Log.i(ExtensionManager.TAG, "Auto-approving install for ${extension.metaData?.name ?: extension.id}")
        return GeckoResult.fromValue(
            WebExtension.PermissionPromptResponse(true, true, true),
        )
    }
}

class ExtensionManager {
    private val runtime get() = requireNotNull(GeckoRuntimeHolder.runtime) { "GeckoRuntime not initialized" }
    private val controller get() = runtime.webExtensionController

    fun prepareController() {
        controller.promptDelegate = BundledExtensionPromptDelegate()
    }

    fun syncExtensions(
        settings: AppSettings,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        prepareController()

        val steps = mutableListOf<() -> GeckoResult<*>>()

        steps += {
            Log.i(TAG, "Installing QStarem bridge extension")
            controller.ensureBuiltIn(ExtensionAssets.QSTAREM_BRIDGE_URI, ExtensionIds.QSTAREM_BRIDGE)
                .then { extension -> enableExtension(extension) }
        }

        if (settings.pStreamEnabled) {
            steps += {
                Log.i(TAG, "Installing P-Stream extension")
                controller.ensureBuiltIn(ExtensionAssets.P_STREAM_URI, ExtensionIds.P_STREAM)
                    .then { extension -> enableExtension(extension) }
            }
        } else {
            steps += { disableIfInstalled(ExtensionIds.P_STREAM) }
        }

        when (settings.adBlocker) {
            AdBlockerChoice.UBLOCK -> {
                steps += { disableIfInstalled(ExtensionIds.ADGUARD) }
                steps += {
                    Log.i(TAG, "Installing uBlock Origin")
                    controller.ensureBuiltIn(ExtensionAssets.UBLOCK_URI, ExtensionIds.UBLOCK)
                        .then { extension -> enableExtension(extension) }
                }
            }
            AdBlockerChoice.ADGUARD -> {
                steps += { disableIfInstalled(ExtensionIds.UBLOCK) }
                steps += {
                    Log.i(TAG, "Installing AdGuard")
                    controller.ensureBuiltIn(ExtensionAssets.ADGUARD_URI, ExtensionIds.ADGUARD)
                        .then { extension -> enableExtension(extension) }
                }
            }
            AdBlockerChoice.NONE -> {
                steps += { disableIfInstalled(ExtensionIds.UBLOCK) }
                steps += { disableIfInstalled(ExtensionIds.ADGUARD) }
            }
        }

        runSteps(steps, 0, onComplete)
    }

    private fun runSteps(
        steps: List<() -> GeckoResult<*>>,
        index: Int,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        if (index >= steps.size) {
            onComplete(Result.success(Unit))
            return
        }

        steps[index]()
            .accept(
                { runSteps(steps, index + 1, onComplete) },
                { error ->
                    Log.e(TAG, "Extension step $index failed", error ?: Exception("Unknown error"))
                    onComplete(Result.failure(error ?: Exception("Unknown error")))
                },
            )
    }

    private fun enableExtension(extension: WebExtension?): GeckoResult<WebExtension?> {
        return if (extension != null) {
            controller.enable(extension, WebExtensionController.EnableSource.APP)
                .then { GeckoResult.fromValue(it) }
        } else {
            GeckoResult.fromValue(null)
        }
    }

    private fun disableIfInstalled(id: String): GeckoResult<WebExtension?> {
        return controller.list().then { extensions ->
            val match = extensions?.find { it.id == id }
            if (match != null) {
                controller.disable(match, WebExtensionController.EnableSource.APP)
            } else {
                GeckoResult.fromValue(null)
            }
        }
    }

    fun clearBrowsingData(onComplete: (Result<Unit>) -> Unit) {
        runtime.storageController.clearData(StorageController.ClearFlags.ALL)
            .accept(
                { onComplete(Result.success(Unit)) },
                { error ->
                    onComplete(Result.failure(error ?: Exception("Failed to clear browsing data")))
                },
            )
    }

    companion object {
        const val TAG = "ExtensionManager"
    }
}
