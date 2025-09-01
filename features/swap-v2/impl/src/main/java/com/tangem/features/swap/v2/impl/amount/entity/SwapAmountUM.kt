package com.tangem.features.swap.v2.impl.amount.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class SwapAmountUM {

    abstract val isPrimaryButtonEnabled: Boolean
    abstract val primaryAmount: SwapAmountFieldUM
    abstract val secondaryAmount: SwapAmountFieldUM
    abstract val selectedAmountType: SwapAmountType
    abstract val swapDirection: SwapDirection

    data class Empty(
        override val swapDirection: SwapDirection,
    ) : SwapAmountUM() {
        override val isPrimaryButtonEnabled = false
        override val selectedAmountType = SwapAmountType.From
        override val primaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.From)
        override val secondaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.To)
    }

    data class Content(
        override val isPrimaryButtonEnabled: Boolean,
        override val swapDirection: SwapDirection,
        override val selectedAmountType: SwapAmountType,

        // two amount fields
        override val primaryAmount: SwapAmountFieldUM,
        override val secondaryAmount: SwapAmountFieldUM,
        val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
        val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus?,

        // selected swap route
        val swapRateType: ExpressRateType,

        // swap models
        val swapCurrencies: SwapCurrencies,
        val swapQuotes: ImmutableList<SwapQuoteUM>,
        val selectedQuote: SwapQuoteUM,
        val showFCAWarning: Boolean,

        // extra data
        val appCurrency: AppCurrency?,
        val showBestRateAnimation: Boolean,
    ) : SwapAmountUM()
}

@Immutable
sealed class SwapAmountFieldUM {
    abstract val amountType: SwapAmountType
    abstract val amountField: AmountState

    data class Empty(
        override val amountType: SwapAmountType,
    ) : SwapAmountFieldUM() {
        override val amountField: AmountState = AmountState.Empty(
            isPrimaryButtonEnabled = false,
            isRedesignEnabled = true,
        )
    }

    data class Loading(
        override val amountType: SwapAmountType,
    ) : SwapAmountFieldUM() {
        override val amountField: AmountState = AmountState.Empty(
            isPrimaryButtonEnabled = false,
            isRedesignEnabled = true,
        )
    }

    data class Content(
        override val amountType: SwapAmountType,
        override val amountField: AmountState,
        val priceImpact: TextReference?,
        val title: TextReference,
        val subtitleLeft: TextReference,
        val subtitleRight: TextReference,
        val subtitleEllipsisLeft: TextEllipsis,
        val subtitleEllipsisRight: TextEllipsis,
        val isClickEnabled: Boolean,
    ) : SwapAmountFieldUM()
}

@Immutable
sealed class PriceImpactUM {

    data object Empty : PriceImpactUM()

    data class Value(val value: Float) : PriceImpactUM()
}

enum class SwapAmountType {
    From,
    To,
}