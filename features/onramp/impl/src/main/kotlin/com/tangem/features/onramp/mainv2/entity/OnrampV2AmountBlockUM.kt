package com.tangem.features.onramp.mainv2.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class OnrampNewAmountBlockUM(
    val currencyUM: OnrampNewCurrencyUM,
    val amountFieldModel: AmountFieldModel,
    val secondaryFieldModel: OnrampNewAmountSecondaryFieldUM,
)

internal data class OnrampNewCurrencyUM(
    val unit: String,
    val code: String,
    val iconUrl: String?,
    val precision: Int,
    val onClick: () -> Unit,
)

@Immutable
internal sealed interface OnrampNewAmountSecondaryFieldUM {
    data object Loading : OnrampNewAmountSecondaryFieldUM
    data class Content(val amount: TextReference) : OnrampNewAmountSecondaryFieldUM
    data class Error(val error: TextReference) : OnrampNewAmountSecondaryFieldUM
}

internal sealed interface OnrampV2AmountButtonUMState {
    data class Loaded(val amountButtons: ImmutableList<OnrampAmountButtonUM>) : OnrampV2AmountButtonUMState
    data object None : OnrampV2AmountButtonUMState
}

internal data class OnrampAmountButtonUM(
    val value: Int,
    val currency: String,
    val onClick: () -> Unit,
)