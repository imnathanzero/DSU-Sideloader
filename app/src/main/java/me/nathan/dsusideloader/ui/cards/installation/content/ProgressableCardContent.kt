package me.nathan.dsusideloader.ui.cards.installation.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.nathan.dsusideloader.ui.components.buttons.PrimaryButton
import me.nathan.dsusideloader.ui.components.buttons.SecondaryButton

@Composable
fun ProgressableCardContent(
    text: String,
    showProgressBar: Boolean = false,
    isIndeterminate: Boolean = false,
    progress: Float = 0F,
    textFirstButton: String = "",
    textSecondButton: String = "",
    onClickFirstButton: (() -> Unit)? = null,
    onClickSecondButton: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (showProgressBar && !isIndeterminate && progress > 0f) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        AnimatedVisibility(visible = showProgressBar) {
            val progressBarModifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))

            if (isIndeterminate) {
                LinearProgressIndicator(
                    modifier = progressBarModifier,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                LinearProgressIndicator(
                    modifier = progressBarModifier,
                    progress = { progress },
                    strokeCap = StrokeCap.Round,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onClickSecondButton != null) {
                SecondaryButton(
                    text = textSecondButton,
                    onClick = onClickSecondButton,
                )
            }
            if (onClickFirstButton != null && onClickSecondButton != null) {
                Spacer(modifier = Modifier.padding(end = 8.dp))
            }
            if (onClickFirstButton != null) {
                PrimaryButton(
                    text = textFirstButton,
                    onClick = onClickFirstButton,
                )
            }
        }
    }
}
