package com.qstarem.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qstarem.app.R
import com.qstarem.app.data.AdBlockerChoice
import com.qstarem.app.data.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onClearData: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var homeUrl by remember(settings.homeUrl) { mutableStateOf(settings.homeUrl) }
    var adBlocker by remember(settings.adBlocker) { mutableStateOf(settings.adBlocker) }
    var pStreamEnabled by remember(settings.pStreamEnabled) { mutableStateOf(settings.pStreamEnabled) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = homeUrl,
                onValueChange = { homeUrl = it },
                label = { Text(stringResource(R.string.home_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            TextButton(onClick = { homeUrl = AppSettings.DEFAULT_HOME_URL }) {
                Text(stringResource(R.string.reset_url))
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.ad_blocker),
                style = MaterialTheme.typography.titleMedium,
            )

            AdBlockerChoice.entries.forEach { choice ->
                val label = when (choice) {
                    AdBlockerChoice.UBLOCK -> stringResource(R.string.adblock_ublock)
                    AdBlockerChoice.ADGUARD -> stringResource(R.string.adblock_adguard)
                    AdBlockerChoice.NONE -> stringResource(R.string.adblock_none)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = adBlocker == choice,
                        onClick = { adBlocker = choice },
                    )
                    Text(text = label)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.pstream_extension))
                Switch(
                    checked = pStreamEnabled,
                    onCheckedChange = { pStreamEnabled = it },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            homeUrl = homeUrl.trim(),
                            adBlocker = adBlocker,
                            pStreamEnabled = pStreamEnabled,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }

            TextButton(
                onClick = onClearData,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.clear_browsing_data))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "QStarem 1.0.0 · GeckoView · P-Stream · uBlock / AdGuard",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
