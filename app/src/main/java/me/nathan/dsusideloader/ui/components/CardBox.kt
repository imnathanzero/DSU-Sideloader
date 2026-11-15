package me.nathan.dsusideloader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CardBox(
    modifier: Modifier = Modifier,
    cardTitle: String = "",
    addToggle: Boolean = false,
    isToggleChecked: Boolean = false,
    isToggleEnabled: Boolean = true,
    addPadding: Boolean = true,
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    onCheckedChange: ((Boolean) -> Unit) = {},
    roundedCornerShape: RoundedCornerShape = RoundedCornerShape(22.dp),
    content: @Composable (ColumnScope) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = roundedCornerShape,
        color = cardColor,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = if (addPadding) 16.dp else 0.dp),
        ) {
            if (cardTitle.isNotEmpty()) {
                if (addToggle) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CardTitle(modifier = Modifier.weight(1F), cardTitle = cardTitle)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Switch(
                            checked = isToggleChecked,
                            onCheckedChange = onCheckedChange,
                            enabled = isToggleEnabled,
                        )
                    }
                } else {
                    CardTitle(
                        cardTitle = cardTitle,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                }
            }
            content(this)
        }
    }
}
