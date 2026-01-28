package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface EarnListUM {
    data object Loading : EarnListUM
    data class Content(val items: ImmutableList<EarnListItemUM>) : EarnListUM
    data class Error(val onRetryClicked: () -> Unit) : EarnListUM
}

@Immutable
internal data class EarnListItemUM(
    val network: TextReference,
    val symbol: TextReference,
    val tokenName: TextReference,
    val currencyIconState: CurrencyIconState,
    val earnValue: TextReference,
    val earnType: TextReference,
    val onItemClick: () -> Unit,
)