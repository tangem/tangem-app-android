package com.tangem.features.onramp.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.extensions.TextReference

internal data class OnrampAmountBlockUM(
    val currencyUM: OnrampCurrencyUM,
    val amountFieldModel: AmountFieldModel,
    val secondaryFieldModel: OnrampAmountSecondaryFieldUM,
)

internal data class OnrampCurrencyUM(val code: String, val iconUrl: String, val precision: Int, val onClick: () -> Unit)

@Immutable
internal sealed interface OnrampAmountSecondaryFieldUM {
    data object Loading : OnrampAmountSecondaryFieldUM
    data class Content(val amount: TextReference) : OnrampAmountSecondaryFieldUM
    data class Error(val error: TextReference) : OnrampAmountSecondaryFieldUM
}