package com.tangem.features.managetokens.entity.customtoken

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.extensions.TextReference

internal data class CustomDerivationInputUM(
    val value: TextFieldValue,
    val error: TextReference? = null,
    val updateValue: (value: TextFieldValue) -> Unit,
    val isConfirmEnabled: Boolean,
    val onConfirm: () -> Unit,
)