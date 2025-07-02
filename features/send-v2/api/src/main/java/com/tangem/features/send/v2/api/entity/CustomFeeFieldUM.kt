package com.tangem.features.send.v2.api.entity

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class CustomFeeFieldUM(
    val value: String,
    val onValueChange: (String) -> Unit,
    val keyboardOptions: KeyboardOptions,
    val keyboardActions: KeyboardActions,
    val symbol: String?,
    val decimals: Int,
    val title: TextReference,
    val footer: TextReference,
    val label: TextReference? = null,
    val isReadonly: Boolean = false,
)