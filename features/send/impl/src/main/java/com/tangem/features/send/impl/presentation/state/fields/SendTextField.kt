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

    /** Label */
    abstract val label: TextReference

    /** Placeholder (hint) */
    abstract val placeholder: TextReference

    data class Amount(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        override val label: TextReference,
        override val placeholder: TextReference,
        val fiatValue: String,
        val isError: Boolean,
        val error: TextReference,
    ) : SendTextField()
}