package me.nathan.dsusideloader.ui.cards.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.nathan.dsusideloader.BuildConfig
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.ui.components.buttons.PrimaryButton
import me.nathan.dsusideloader.ui.components.buttons.SecondaryButton
import me.nathan.dsusideloader.ui.screen.about.UpdateStatus
import me.nathan.dsusideloader.ui.screen.about.UpdaterCardState

@Composable
fun AppHeaderCard(
    onClickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "iconScale",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        isPressed = true
                        onClickImage()
                        scope.launch {
                            delay(120)
                            isPressed = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape),
                    painter = painterResource(id = R.drawable.app_icon_mini),
                    contentDescription = "App icon",
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun SoftwareUpdateCard(
    uiState: UpdaterCardState,
    onClickCheckUpdates: () -> Unit,
    onClickDownloadUpdate: () -> Unit,
    onClickViewChangelog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isChecking = uiState.updateStatus == UpdateStatus.CHECKING_FOR_UPDATES
    val isUpdateFound = uiState.updateStatus == UpdateStatus.UPDATE_FOUND
    val isUpToDate = uiState.updateStatus == UpdateStatus.NO_UPDATE_FOUND
    val isDownloading = uiState.isDownloading

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isChecking && !isDownloading) { onClickCheckUpdates() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val (icon, iconTint, containerTint) = when {
                    isChecking -> Triple(
                        Icons.Outlined.Refresh,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    )
                    isDownloading -> Triple(
                        Icons.Outlined.Download,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    )
                    isUpdateFound -> Triple(
                        Icons.Outlined.NewReleases,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    )
                    isUpToDate -> Triple(
                        Icons.Outlined.CheckCircle,
                        Color(0xFF4CAF50),
                        Color(0xFF4CAF50).copy(alpha = 0.15f),
                    )
                    else -> Triple(
                        Icons.Outlined.SystemUpdate,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(containerTint),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val titleText = when {
                        isChecking -> stringResource(R.string.check_updates_text_checking)
                        isDownloading -> stringResource(R.string.downloading_update)
                        isUpdateFound -> stringResource(R.string.check_updates_status_update_available)
                        isUpToDate -> stringResource(R.string.check_updates_status_up_to_date)
                        else -> stringResource(R.string.check_updates_title)
                    }

                    val descText = when {
                        isChecking -> stringResource(R.string.check_updates_text_checking_description)
                        isDownloading -> "${(uiState.progressBar * 100).toInt()}% completed"
                        isUpdateFound -> stringResource(R.string.check_updates_text_found, uiState.updateVersion)
                        isUpToDate -> stringResource(R.string.check_updates_text_updated)
                        else -> stringResource(R.string.check_updates_text_idle)
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUpdateFound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (!isChecking && !isDownloading) {
                    FilledTonalIconButton(
                        onClick = onClickCheckUpdates,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.check_updates_title),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isDownloading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    LinearProgressIndicator(
                        progress = { uiState.progressBar.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
            }

            AnimatedVisibility(
                visible = isUpdateFound && !isDownloading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(
                        text = stringResource(id = R.string.changelog),
                        onClick = onClickViewChangelog,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PrimaryButton(
                        text = stringResource(id = R.string.download),
                        onClick = onClickDownloadUpdate,
                    )
                }
            }
        }
    }
}

@Composable
fun UpdaterCard(
    uiState: UpdaterCardState,
    isUpdaterAvailable: Boolean,
    onClickImage: () -> Unit,
    onClickCheckUpdates: () -> Unit,
    onClickDownloadUpdate: () -> Unit,
    onClickViewChangelog: () -> Unit,
) {
    AppHeaderCard(onClickImage = onClickImage)

    if (isUpdaterAvailable) {
        SoftwareUpdateCard(
            uiState = uiState,
            onClickCheckUpdates = onClickCheckUpdates,
            onClickDownloadUpdate = onClickDownloadUpdate,
            onClickViewChangelog = onClickViewChangelog,
        )
    }
}
