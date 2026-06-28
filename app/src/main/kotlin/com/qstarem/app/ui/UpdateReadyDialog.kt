package com.qstarem.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.qstarem.app.update.UpdatePhase
import com.qstarem.app.update.UpdateUiState

@Composable
fun UpdateReadyDialog(
    state: UpdateUiState,
    visible: Boolean,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible || state.phase != UpdatePhase.Ready) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ready") },
        text = {
            Text(
                state.message
                    ?: "QStarem ${state.availableVersion ?: ""} is ready to install.",
            )
        },
        confirmButton = {
            Button(onClick = onInstall) {
                Text("Install now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        },
    )
}
