package com.tangem.tap.features.walletSelector.ui.model

internal sealed interface DialogModel {
    data class RenameWalletDialog(
        val currentName: String,
        val onConfirm: (newName: String) -> Unit,
        val onDismiss: () -> Unit,
    ) : DialogModel

    data class RemoveWalletDialog(
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit,
    ) : DialogModel
}
