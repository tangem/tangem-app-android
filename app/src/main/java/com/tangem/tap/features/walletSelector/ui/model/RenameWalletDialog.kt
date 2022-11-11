package com.tangem.tap.features.walletSelector.ui.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class RenameWalletDialog(
    val currentName: String,
    val onApply: (newName: String) -> Unit,
    val onCancel: () -> Unit,
)
