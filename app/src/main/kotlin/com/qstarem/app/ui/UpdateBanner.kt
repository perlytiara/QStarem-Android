package com.qstarem.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qstarem.app.update.UpdatePhase
import com.qstarem.app.update.UpdateUiState

@Composable
fun UpdateBanner(
    state: UpdateUiState,
    onConfirmDownload: () -> Unit,
    onDismissDownload: () -> Unit,
    onInstallUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.phase == UpdatePhase.Idle || state.phase == UpdatePhase.Checking) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF12121A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = state.message ?: "Update available",
            color = Color(0xFFF5F5FA),
            style = MaterialTheme.typography.bodyMedium,
        )

        when (state.phase) {
            UpdatePhase.AwaitingDownloadConsent -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConfirmDownload) {
                        Text("Download")
                    }
                    OutlinedButton(onClick = onDismissDownload) {
                        Text("Later")
                    }
                }
            }
            UpdatePhase.Ready -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onInstallUpdate) {
                        Text("Install now")
                    }
                    OutlinedButton(onClick = onDismissDownload) {
                        Text("Later")
                    }
                }
            }
            UpdatePhase.Downloading -> {
                Text(
                    text = "${(state.progress * 100).toInt()}%",
                    color = Color(0xFFB8B8C8),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            UpdatePhase.Error -> Unit
            else -> Unit
        }
    }
}
