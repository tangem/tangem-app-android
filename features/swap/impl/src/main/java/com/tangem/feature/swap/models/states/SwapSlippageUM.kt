package com.tangem.feature.swap.models.states

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface SwapSlippageUM {

    data object Hidden : SwapSlippageUM

    /**
     * [value] is the provider's default slippage until the user picks a custom one, then the picked percent.
     * [isHighlighted] marks a custom slippage that was just reset to the default.
     */
    data class Content(
        val value: TextReference,
        val isHighlighted: Boolean,
        val onClick: () -> Unit,
    ) : SwapSlippageUM
}

/** [warning] flags an accepted but risky value under the field; [error] takes its place when both are set. */
internal data class SwapSlippageBottomSheetConfig(
    val valueState: Value,
    val onConfirmClick: () -> Unit,
    val error: TextReference?,
    val warning: TextReference? = null,
) : TangemBottomSheetConfigContent {

    internal sealed interface Value {
        data class ReadOnly(val slippage: TextReference) : Value {
            val info: TextReference = resourceReference(R.string.swap_custom_slippage_info, wrappedList(slippage))
        }

        data class Edit(
            val value: String,
            val presets: ImmutableList<SwapSlippagePresetUM>,
            val onValueChange: (String) -> Unit,
            val isConfirmEnabled: Boolean,
        ) : Value
    }
}

@Immutable
internal data class SwapSlippagePresetUM(
    val value: String,
    val label: TextReference,
    val onClick: () -> Unit,
)