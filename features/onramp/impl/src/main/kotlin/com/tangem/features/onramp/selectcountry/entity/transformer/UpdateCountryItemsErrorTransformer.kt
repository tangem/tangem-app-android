package com.tangem.features.onramp.selectcountry.entity.transformer

import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.utils.transformer.Transformer

internal class UpdateCountryItemsErrorTransformer(
    private val onRetry: () -> Unit,
) : Transformer<CountryListUM> {
    override fun transform(prevState: CountryListUM): CountryListUM {
        return CountryListUM.Error(
            searchBarUM = prevState.searchBarUM,
            onRetry = onRetry,
        )
    }
}