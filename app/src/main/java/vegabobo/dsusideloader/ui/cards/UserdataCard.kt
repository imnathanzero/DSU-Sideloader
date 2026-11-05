package vegabobo.dsusideloader.ui.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.components.CardBox
import vegabobo.dsusideloader.ui.components.FileSelectionBox
import vegabobo.dsusideloader.ui.screen.home.UserDataCardState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserdataCard(
    isEnabled: Boolean,
    uiState: UserDataCardState,
    isDsuInstalled: Boolean = false,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit = {},
    onPreserveCheckedChange: (Boolean) -> Unit = {},
) {
    val quickPresets = listOf("2", "4", "8", "16", "32")

    if (isDsuInstalled && !isEnabled) {
        CardBox(
            modifier = modifier,
            cardTitle = stringResource(id = R.string.userdata_size),
            addToggle = false,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Preserve userdata item
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isEnabled) { onPreserveCheckedChange(!uiState.preserveSelected) }
                        .padding(vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.preserve_userdata),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(id = R.string.preserve_userdata_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = uiState.preserveSelected,
                        enabled = !isEnabled,
                        onCheckedChange = { onPreserveCheckedChange(!uiState.preserveSelected) },
                    )
                }

                AnimatedVisibility(visible = !uiState.preserveSelected) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        FileSelectionBox(
                            modifier = Modifier.padding(bottom = 6.dp),
                            isEnabled = !isEnabled,
                            isError = uiState.isError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textFieldValue = uiState.text,
                            textFieldTitle = stringResource(id = R.string.userdata_size_info),
                            onValueChange = onValueChange,
                        )

                        // Quick size presets
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            for (preset in quickPresets) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (uiState.text == preset) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    modifier = Modifier.clickable(enabled = !isEnabled) {
                                        onValueChange(preset)
                                    },
                                ) {
                                    Text(
                                        text = "$preset GB",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (uiState.text == preset) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.isError) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        id = R.string.allowed_userdata_allocation,
                                        uiState.maximumAllowed,
                                    ),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        CardBox(
            modifier = modifier,
            cardTitle = stringResource(id = R.string.userdata_size),
            addToggle = true,
            isToggleEnabled = !isEnabled,
            isToggleChecked = uiState.isSelected,
            onCheckedChange = onCheckedChange,
        ) {
            AnimatedVisibility(
                visible = uiState.isSelected,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    FileSelectionBox(
                        modifier = Modifier.padding(bottom = 6.dp),
                        isEnabled = !isEnabled,
                        isError = uiState.isError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textFieldValue = uiState.text,
                        textFieldTitle = stringResource(id = R.string.userdata_size_info),
                        onValueChange = onValueChange,
                    )

                    // Quick size presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        for (preset in quickPresets) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.text == preset) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier.clickable(enabled = !isEnabled) {
                                    onValueChange(preset)
                                },
                            ) {
                                Text(
                                    text = "$preset GB",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.text == preset) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.isError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.allowed_userdata_allocation,
                                    uiState.maximumAllowed,
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
