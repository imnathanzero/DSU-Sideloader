package vegabobo.dsusideloader.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun DynamicListItem(
    listLength: Int,
    currentValue: Int,
    content: @Composable () -> Unit,
) {
    val shape = when {
        listLength == 0 -> RoundedCornerShape(24.dp)
        currentValue == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        currentValue == listLength -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 24.dp, bottomStart = 24.dp)
        else -> RoundedCornerShape(4.dp)
    }
    CardBox(
        addPadding = false,
        roundedCornerShape = shape,
    ) {
        content()
    }
}
