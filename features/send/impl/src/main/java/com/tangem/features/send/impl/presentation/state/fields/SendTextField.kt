package com.tangem.features.send.impl.presentation.state.fields

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

    // /** Placeholder (hint) */
    // abstract val placeholder: TextReference

    data class Amount(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        val placeholder: TextReference,
        val fiatValue: String,
        val isError: Boolean,
        val error: TextReference,
    ) : SendTextField()

    data class RecipientAddress(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        val placeholder: TextReference,
        val label: TextReference,
        val isError: Boolean = false,
        val error: TextReference? = null,
    ) : SendTextField()

    data class RecipientMemo(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        val placeholder: TextReference,
        val label: TextReference,
        val isError: Boolean = false,
        val error: TextReference? = null,
    ) : SendTextField()

    data class CustomFee(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        val label: TextReference? = null,
    ) : SendTextField()
}
