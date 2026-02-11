package com.tangem.features.feed.ui.feed.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.collections.immutable.ImmutableList

internal data class EarnListUM(
    val items: ImmutableList<EarnListItemUM>,
    val contentState: EarnListContentState,
)

@Immutable
internal data class EarnListItemUM(
    val network: TextReference.Str,
    val symbol: String,
    val tokenName: String,
    val currencyIconState: CurrencyIconState,
    val earnValue: EarnValueUM,
    val earnType: EarnType,
    val onItemClick: () -> Unit,
)

@Immutable
internal data class EarnValueUM(
    val percent: SerializedBigDecimal,
    val earnValueType: EarnValueType,
)

internal enum class EarnType {
    Staking, Yield
}

internal enum class EarnValueType {
    APR, APY
}

@Immutable
internal sealed interface EarnListContentState {
    data object Loading : EarnListContentState
    data object Content : EarnListContentState
    data class Error(val onRetryClicked: () -> Unit) : EarnListContentState
}