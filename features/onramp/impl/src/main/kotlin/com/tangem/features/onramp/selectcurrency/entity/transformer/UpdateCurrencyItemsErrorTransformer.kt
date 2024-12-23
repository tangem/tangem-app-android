package com.tangem.features.onramp.selectcurrency.entity.transformer

import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import com.tangem.utils.transformer.Transformer

internal class UpdateCurrencyItemsErrorTransformer(
    private val onRetry: () -> Unit,
) : Transformer<CurrenciesListUM> {
    override fun transform(prevState: CurrenciesListUM): CurrenciesListUM {
        return CurrenciesListUM.Error(
            searchBarUM = prevState.searchBarUM,
            onRetry = onRetry,
        )
    }
}