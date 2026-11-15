package me.nathan.dsusideloader.ui.cards.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.nathan.dsusideloader.BuildConfig
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.ui.components.PreferenceItem
import me.nathan.dsusideloader.ui.screen.about.UpdateStatus
import me.nathan.dsusideloader.ui.screen.about.UpdaterCardState

@Composable
fun UpdaterCard(
    uiState: UpdaterCardState,
    isUpdaterAvailable: Boolean,
    onClickImage: () -> Unit,
    onClickCheckUpdates: () -> Unit,
    onClickDownloadUpdate: () -> Unit,
    onClickViewChangelog: () -> Unit,
) {
    fun isDownloading(): Boolean =
        uiState.isDownloading || uiState.updateStatus == UpdateStatus.CHECKING_FOR_UPDATES

    fun isCheckingForUpdates(): Boolean =
        uiState.updateStatus == UpdateStatus.CHECKING_FOR_UPDATES

    fun isUpdateFound(): Boolean =
        uiState.updateStatus == UpdateStatus.UPDATE_FOUND

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val progressBarModifier = Modifier.size(104.dp)
                if (isCheckingForUpdates()) {
                    CircularProgressIndicator(
                        modifier = progressBarModifier,
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                    )
                }
                if (uiState.isDownloading) {
                    CircularProgressIndicator(
                        progress = { uiState.progressBar },
                        modifier = progressBarModifier,
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp,
                    )
                }

                val selected = remember { mutableStateOf(false) }
                val scale = animateFloatAsState(if (selected.value) 0.85f else 1f)
                selected.value = isDownloading()
                Image(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .clickable { onClickImage() },
                    painter = painterResource(id = R.drawable.app_icon_mini),
                    contentDescription = "App icon",
                )
            }

            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    id = R.string.version_info,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.75f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isUpdaterAvailable) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
                PreferenceItem(
                    title = stringResource(id = R.string.check_updates_title),
                    description =
                    when (uiState.updateStatus) {
                        UpdateStatus.NO_UPDATE_FOUND ->
                            stringResource(id = R.string.check_updates_text_updated)

                        UpdateStatus.UPDATE_FOUND ->
                            stringResource(R.string.check_updates_text_found, uiState.updateVersion)

                        else ->
                            stringResource(id = R.string.check_updates_text_idle)
                    },
                    icon = Icons.Outlined.SystemUpdate,
                    onClick = { onClickCheckUpdates() },
                )
                AnimatedVisibility(visible = isUpdateFound()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { onClickViewChangelog() },
                        ) {
                            Text(text = stringResource(id = R.string.changelog))
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        ElevatedButton(
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            onClick = { onClickDownloadUpdate() },
                        ) {
                            Text(text = stringResource(id = R.string.download))
                        }
                    }
                }
            }
        }
    }
}
