package me.nathan.dsusideloader.ui.cards.installation.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.ui.components.FileSelectionBox
import me.nathan.dsusideloader.ui.components.buttons.ErrorButton
import me.nathan.dsusideloader.ui.components.buttons.PrimaryButton
import me.nathan.dsusideloader.ui.components.buttons.SecondaryButton
import me.nathan.dsusideloader.ui.screen.home.InstallationCardState

@Composable
fun DsuInstalledCardContent(
    textFieldInteraction: MutableInteractionSource,
    uiState: InstallationCardState,
    onClickClear: () -> Unit,
    onClickInstall: () -> Unit,
    onClickRebootToDynOS: () -> Unit,
    onClickDiscardDsu: () -> Unit,
) {
    Text(
        text = stringResource(R.string.dsu_already_installed),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    FileSelectionBox(
        textFieldInteraction = textFieldInteraction,
        isEnabled = uiState.isTextFieldEnabled,
        isError = uiState.isError,
        isReadOnly = true,
        textFieldValue = uiState.text,
        textFieldTitle = stringResource(id = R.string.select_gsi_info),
    )

    if (uiState.selectedFiles.size > 1) {
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (file in uiState.selectedFiles) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = file.partitionName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (file.formattedSize.isNotEmpty()) {
                        Text(
                            text = file.formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.padding(top = 10.dp))

    // Install / Update actions row when file is selected
    if (uiState.isInstallable) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedVisibility(visible = uiState.isError) {
                Text(
                    text = stringResource(id = R.string.file_unsupported),
                    modifier = Modifier.padding(start = 2.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.weight(1F))
            SecondaryButton(
                text = stringResource(R.string.clear),
                onClick = onClickClear,
            )
            Spacer(modifier = Modifier.padding(end = 6.dp))
            PrimaryButton(
                text = stringResource(R.string.update),
                onClick = onClickInstall,
            )
        }
        Spacer(modifier = Modifier.padding(top = 8.dp))
    }

    // Reboot / Discard existing DSU actions row
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(modifier = Modifier.weight(1F))
        SecondaryButton(
            text = stringResource(id = R.string.reboot_into_dsu),
            onClick = onClickRebootToDynOS,
        )
        Spacer(modifier = Modifier.padding(end = 6.dp))
        ErrorButton(
            text = stringResource(id = R.string.discard),
            onClick = onClickDiscardDsu,
        )
    }
}
