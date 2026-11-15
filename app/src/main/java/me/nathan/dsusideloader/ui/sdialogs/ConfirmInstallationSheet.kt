package me.nathan.dsusideloader.ui.sdialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.nathan.dsusideloader.R
import me.nathan.dsusideloader.ui.components.DialogItem
import me.nathan.dsusideloader.ui.components.DialogLikeBottomSheet

@Composable
fun ConfirmInstallationSheet(
    filename: String,
    userdata: String,
    onClickConfirm: () -> Unit,
    onClickCancel: () -> Unit,
) {
    DialogLikeBottomSheet(
        title = stringResource(id = R.string.proceed_installation),
        icon = Icons.Outlined.InstallMobile,
        text = stringResource(id = R.string.proceed_installation_description),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                DialogItem(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    title = "${stringResource(id = R.string.selected_file)}:",
                    text = filename,
                    textColor = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                DialogItem(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Storage,
                    title = "${stringResource(id = R.string.userdata_size)}:",
                    text = "${userdata}GB",
                    textColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmText = stringResource(id = R.string.proceed),
        cancelText = stringResource(id = R.string.cancel),
        onClickConfirm = { onClickConfirm() },
        onClickCancel = { onClickCancel() },
    )
}
