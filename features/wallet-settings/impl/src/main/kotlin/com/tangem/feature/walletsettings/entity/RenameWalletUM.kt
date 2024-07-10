package com.tangem.feature.walletsettings.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

@Immutable
internal data class RenameWalletUM(
    val walletNameValue: TextFieldValue,
    val isNameCorrect: Boolean,
    val updateValue: (value: TextFieldValue) -> Unit,
    val onConfirm: () -> Unit,
)
