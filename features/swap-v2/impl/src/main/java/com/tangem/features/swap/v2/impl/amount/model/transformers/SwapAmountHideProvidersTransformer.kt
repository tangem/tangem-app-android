package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.domain.swap.models.SwapCryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SwapAmountHideProvidersTransformer(
    private val providerIds: Set<String>,
    private val secondaryMaximumAmountBoundary: EnterAmountBoundary?,
    private val secondaryMinimumAmountBoundary: EnterAmountBoundary?,
    private val isNeedApplyFCARestrictions: Boolean,
    private val isBalanceHidden: Boolean,
    private val primaryFiatRateUSD: BigDecimal?,
    private val secondaryFiatRateUSD: BigDecimal?,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val quotes = prevState.swapQuotes.filterNot { quote -> quote.provider?.providerId in providerIds }
        val state = prevState.copy(
            swapCurrencies = prevState.swapCurrencies.withoutProviders(),
            swapQuotes = quotes.toPersistentList(),
        )
        if (prevState.selectedQuote.provider?.providerId !in providerIds) return state

        return SwapAmountSelectQuoteTransformer(
            quoteUM = quotes.firstOrNull { quote -> quote is SwapQuoteUM.Content } ?: SwapQuoteUM.Empty,
            secondaryMaximumAmountBoundary = secondaryMaximumAmountBoundary,
            secondaryMinimumAmountBoundary = secondaryMinimumAmountBoundary,
            isNeedApplyFCARestrictions = isNeedApplyFCARestrictions,
            isBalanceHidden = isBalanceHidden,
            primaryFiatRateUSD = primaryFiatRateUSD,
            secondaryFiatRateUSD = secondaryFiatRateUSD,
        ).transform(state)
    }

    private fun SwapCurrencies.withoutProviders(): SwapCurrencies {
        return copy(fromGroup = fromGroup.withoutProviders(), toGroup = toGroup.withoutProviders())
    }

    private fun SwapCurrenciesGroup.withoutProviders(): SwapCurrenciesGroup {
        return copy(
            available = available.withoutProviders(),
            unavailable = unavailable.withoutProviders(),
            availableForSwap = availableForSwap.withoutProviders(),
        )
    }

    private fun List<SwapCryptoCurrency>.withoutProviders(): List<SwapCryptoCurrency> {
        return map { currency ->
            currency.copy(providers = currency.providers.filterNot { provider -> provider.providerId in providerIds })
        }
    }
}