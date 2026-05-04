package com.tangem.features.hotwallet.walletbackup.ui.component

import androidx.compose.runtime.Composable
import com.tangem.common.R
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.extensions.stringResourceSafe

@Composable
fun GoogleDriveFakeDoorDialog(onDismiss: () -> Unit) {
    BasicDialog(
        title = stringResourceSafe(id = R.string.hw_backup_google_drive_dialog_title),
        message = stringResourceSafe(id = R.string.hw_backup_google_drive_dialog_message),
        confirmButton = DialogButtonUM(onClick = onDismiss),
        onDismissDialog = onDismiss,
    )
}