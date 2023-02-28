package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.BasicDialog
import com.tangem.core.ui.components.DialogButton
import com.tangem.tap.features.walletSelector.ui.model.DialogModel
import com.tangem.wallet.R

@Composable
internal fun RemoveWalletDialogContent(dialog: DialogModel.RemoveWalletDialog) {
    BasicDialog(
        message = stringResource(id = R.string.user_wallet_list_delete_prompt),
        confirmButton = DialogButton(
            title = stringResource(id = R.string.common_delete),
            warning = true,
            onClick = dialog.onConfirm,
        ),
        dismissButton = DialogButton(
            onClick = dialog.onDismiss,
        ),
        onDismissDialog = dialog.onDismiss,
    )
}
