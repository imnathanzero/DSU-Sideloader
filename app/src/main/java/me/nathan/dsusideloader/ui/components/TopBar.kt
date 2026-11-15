package me.nathan.dsusideloader.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior? = null,
    barTitle: String,
    statusChip: @Composable (() -> Unit)? = null,
    icon: ImageVector? = null,
    iconContentDescription: String? = "icon",
    onClickIcon: () -> Unit = {},
    onClickBackButton: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    LargeTopAppBar(
        title = {
            Text(
                text = barTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        navigationIcon = {
            if (onClickBackButton != null) {
                IconButton(onClickBackButton) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = iconContentDescription,
                    )
                }
            }
        },
        actions = {
            if (statusChip != null) {
                statusChip()
            }
            if (actions != null) {
                actions()
            } else if (icon != null) {
                IconButton(onClickIcon) {
                    Icon(
                        imageVector = icon,
                        contentDescription = iconContentDescription,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        scrollBehavior = scrollBehavior,
    )
}
