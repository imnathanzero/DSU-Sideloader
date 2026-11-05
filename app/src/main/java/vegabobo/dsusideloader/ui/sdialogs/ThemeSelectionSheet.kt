package vegabobo.dsusideloader.ui.sdialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vegabobo.dsusideloader.R
import vegabobo.dsusideloader.ui.components.DialogLikeBottomSheet
import vegabobo.dsusideloader.ui.theme.ThemeMode

@Composable
fun ThemeSelectionSheet(
    currentTheme: ThemeMode,
    onSelectTheme: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    DialogLikeBottomSheet(
        title = stringResource(id = R.string.theme),
        icon = Icons.Outlined.Palette,
        content = {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                ThemeOptionRow(
                    title = stringResource(id = R.string.theme_system),
                    icon = Icons.Outlined.BrightnessAuto,
                    selected = currentTheme == ThemeMode.SYSTEM,
                    onClick = { onSelectTheme(ThemeMode.SYSTEM) },
                )
                Spacer(modifier = Modifier.height(4.dp))
                ThemeOptionRow(
                    title = stringResource(id = R.string.theme_light),
                    icon = Icons.Outlined.LightMode,
                    selected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onSelectTheme(ThemeMode.LIGHT) },
                )
                Spacer(modifier = Modifier.height(4.dp))
                ThemeOptionRow(
                    title = stringResource(id = R.string.theme_dark),
                    icon = Icons.Outlined.DarkMode,
                    selected = currentTheme == ThemeMode.DARK,
                    onClick = { onSelectTheme(ThemeMode.DARK) },
                )
            }
        },
        confirmText = stringResource(id = R.string.close),
        onClickConfirm = onDismiss,
    )
}

@Composable
private fun ThemeOptionRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1F),
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}
