package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class QuickTopUpBlockUM(
    val amounts: ImmutableList<QuickTopUpAmountUM>,
) {
    @Immutable
    data class QuickTopUpAmountUM(
        val displayValue: TextReference,
        val onClick: () -> Unit,
        val isOther: Boolean = false,
    )
}