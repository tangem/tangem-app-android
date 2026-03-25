package com.tangem.features.onramp.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class OnrampAmountBlockUM(
    val currencyUM: OnrampCurrencyUM,
    val amountFieldModel: AmountFieldModel,
    val secondaryFieldModel: OnrampSecondaryFieldErrorUM,
)

internal data class OnrampCurrencyUM(
    val unit: String,
    val code: String,
    val iconUrl: String?,
    val precision: Int,
    val onClick: () -> Unit,
)

@Immutable
internal sealed interface OnrampSecondaryFieldErrorUM {
    data object Empty : OnrampSecondaryFieldErrorUM
    data class Error(val error: TextReference) : OnrampSecondaryFieldErrorUM
}

internal sealed interface OnrampAmountButtonUMState {
    data class Loaded(val amountButtons: ImmutableList<OnrampAmountButtonUM>) : OnrampAmountButtonUMState
    data object None : OnrampAmountButtonUMState
}

internal data class OnrampAmountButtonUM(
    val value: Int,
    val currency: String,
    val onClick: () -> Unit,
)