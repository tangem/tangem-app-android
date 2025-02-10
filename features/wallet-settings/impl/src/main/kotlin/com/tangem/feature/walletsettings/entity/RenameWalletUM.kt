package com.tangem.feature.walletsettings.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class RenameWalletUM(
    val walletNameValue: TextFieldValue,
    val onValueChange: (value: TextFieldValue) -> Unit,
    val isConfirmEnabled: Boolean,
    val onConfirmClick: () -> Unit,
    val error: TextReference?,
)