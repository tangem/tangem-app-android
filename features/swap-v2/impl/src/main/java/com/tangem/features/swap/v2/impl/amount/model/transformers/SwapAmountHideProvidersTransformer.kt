package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.domain.swap.models.SwapCryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.domain.swap.models.SwapDirection.Companion.withSwapDirection
import com.tangem.domain.swap.models.SwapRateMode
import com.tangem.domain.swap.models.getGroupWithDirection
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class SwapAmountHideProvidersTransformer(
    private val providerIds: Set<String>,
) : Transformer<SwapAmountUM> {

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        if (prevState !is SwapAmountUM.Content) return prevState

        val quotes = prevState.swapQuotes.filterNot { quote -> quote.provider?.providerId in providerIds }
        val swapCurrencies = prevState.swapCurrencies.withoutProviders()
        return prevState.copy(
            swapCurrencies = swapCurrencies,
            swapQuotes = quotes.toPersistentList(),
            swapRateMode = prevState.getSwapRateMode(swapCurrencies),
        )
    }

    private fun SwapAmountUM.Content.getSwapRateMode(swapCurrencies: SwapCurrencies): SwapRateMode {
        val toCurrency = swapDirection.withSwapDirection(
            onDirect = { secondaryCryptoCurrencyStatus?.currency },
            onReverse = { primaryCryptoCurrencyStatus.currency },
        ) ?: return swapRateMode

        val providers = swapCurrencies.getGroupWithDirection(swapDirection).available
            .filter { swapCurrency -> swapCurrency.currencyStatus.currency.id == toCurrency.id }
            .flatMap(SwapCryptoCurrency::providers)
        return if (providers.isEmpty()) swapRateMode else providers.getSwapRateMode()
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