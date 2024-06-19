package com.tangem.features.send.impl.presentation.state.fields

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class SendTextField {

    /** Current value */
    abstract val value: String

    /** Lambda be invoked when value is been changed */
    abstract val onValueChange: (String) -> Unit

    /** Keyboard options */
    abstract val keyboardOptions: KeyboardOptions

    data class RecipientAddress(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        val placeholder: TextReference,
        val label: TextReference,
        val isError: Boolean = false,
        val error: TextReference? = null,
        val isValuePasted: Boolean,
    ) : SendTextField()

    data class RecipientMemo(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        val placeholder: TextReference,
        val label: TextReference,
        val isError: Boolean = false,
        val error: TextReference? = null,
        val disabledText: TextReference,
        val isEnabled: Boolean,
        val isValuePasted: Boolean,
    ) : SendTextField()

    data class CustomFee(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        val keyboardActions: KeyboardActions,
        val symbol: String?,
        val decimals: Int,
        val title: TextReference,
        val footer: TextReference,
        val label: TextReference? = null,
        val isReadonly: Boolean = false,
    ) : SendTextField()
}
