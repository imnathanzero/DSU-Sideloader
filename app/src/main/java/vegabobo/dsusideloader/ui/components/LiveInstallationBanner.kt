package vegabobo.dsusideloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.installer.LiveInstallationState
import vegabobo.dsusideloader.ui.screen.Destinations

@Composable
fun LiveInstallationBanner(
    liveState: LiveInstallationState,
    currentDestination: String,
    onNavigateHome: () -> Unit,
    onCancel: () -> Unit,
    onReboot: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVisible = currentDestination != Destinations.Homepage &&
        currentDestination != Destinations.Install &&
        (liveState.isInstalling || liveState.isSuccess || liveState.isError)

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hasBottomBar = currentDestination == Destinations.Homepage ||
        currentDestination == Destinations.Install ||
        currentDestination == Destinations.Systems ||
        currentDestination == Destinations.Preferences ||
        currentDestination == Destinations.Partitions
    val bottomOffset = if (hasBottomBar) 88.dp + navBarPadding else 16.dp + navBarPadding

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
        modifier = modifier,
    ) {
        val containerColor = when {
            liveState.isSuccess -> MaterialTheme.colorScheme.primaryContainer
            liveState.isError -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val contentColor = when {
            liveState.isSuccess -> MaterialTheme.colorScheme.onPrimaryContainer
            liveState.isError -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurface
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = bottomOffset),
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onNavigateHome() },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left Status Indicator
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            liveState.isSuccess -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            liveState.isError -> {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            liveState.isIndeterminate -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            else -> {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = liveState.progress,
                                    label = "banner_progress_circle",
                                )
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Center Information Column
                    Column(
                        modifier = Modifier.weight(1F),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val titleText = when {
                            liveState.isSuccess -> stringResource(R.string.notification_completed_title)
                            liveState.isError -> stringResource(R.string.notification_failed_title)
                            liveState.partition.isNotEmpty() -> stringResource(
                                R.string.installing_partition,
                                liveState.partition,
                            )
                            liveState.title.isNotEmpty() -> liveState.title
                            else -> stringResource(R.string.notification_installing_title)
                        }

                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        if (liveState.isInstalling) {
                            val percentage = (liveState.progress * 100).toInt()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!liveState.isIndeterminate) {
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor.copy(alpha = 0.8F),
                                    )
                                    val animatedLinearProgress by animateFloatAsState(
                                        targetValue = liveState.progress,
                                        label = "banner_linear_progress",
                                    )
                                    LinearProgressIndicator(
                                        progress = { animatedLinearProgress },
                                        modifier = Modifier
                                            .weight(1F)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Text(
                                        text = liveState.stepText.ifEmpty {
                                            stringResource(R.string.processing)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor.copy(alpha = 0.8F),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else if (liveState.isSuccess) {
                            Text(
                                text = stringResource(R.string.live_alert_banner_ready),
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.85F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else if (liveState.isError) {
                            Text(
                                text = liveState.errorText.ifEmpty {
                                    stringResource(R.string.unknown_error)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.85F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Actions
                    when {
                        liveState.isInstalling -> {
                            OutlinedButton(
                                onClick = onCancel,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(34.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        liveState.isSuccess && liveState.canReboot -> {
                            FilledTonalButton(
                                onClick = onReboot,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.reboot_into_dsu),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        liveState.isError || liveState.isSuccess -> {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.close),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
