package com.tangem.features.onramp.selectcurrency.entity

import androidx.compose.runtime.Immutable
import com.tangem.domain.onramp.model.OnrampCurrency

@Immutable
internal sealed interface CurrencyItemState {

    /** Unique ID */
    val id: String

    data class Loading(override val id: String) : CurrencyItemState

    data class Content(
        override val id: String,
        val onrampCurrency: OnrampCurrency,
        val onClick: () -> Unit,
    ) : CurrencyItemState
}