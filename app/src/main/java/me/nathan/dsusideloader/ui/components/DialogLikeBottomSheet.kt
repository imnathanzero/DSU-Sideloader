package me.nathan.dsusideloader.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.nathan.dsusideloader.ui.components.buttons.PrimaryButton
import me.nathan.dsusideloader.ui.components.buttons.SecondaryButton

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DialogLikeBottomSheet(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String = "",
    text: String = "",
    confirmText: String = "",
    cancelText: String = "",
    hideKeyboard: Boolean = true,
    onClickConfirm: () -> Unit = {},
    onClickCancel: () -> Unit = {},
    onDismiss: () -> Unit = onClickCancel,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    if (hideKeyboard) {
        LocalSoftwareKeyboardController.current?.hide()
        LocalFocusManager.current.clearFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = if (title.isNotEmpty()) {
            {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            null
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                if (text.isNotEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content()
            }
        },
        confirmButton = {
            if (confirmText.isNotEmpty()) {
                PrimaryButton(
                    text = confirmText,
                    onClick = onClickConfirm,
                )
            }
        },
        dismissButton = {
            if (cancelText.isNotEmpty()) {
                SecondaryButton(
                    text = cancelText,
                    onClick = onClickCancel,
                )
            }
        },
    )
}
