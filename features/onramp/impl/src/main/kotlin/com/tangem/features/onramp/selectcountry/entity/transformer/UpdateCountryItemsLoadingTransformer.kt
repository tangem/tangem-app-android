package com.tangem.features.onramp.selectcountry.entity.transformer

import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

internal class UpdateCountryItemsLoadingTransformer(
    private val loadingItems: ImmutableList<CountryItemState.Loading>,
) : Transformer<CountryListUM> {
    override fun transform(prevState: CountryListUM): CountryListUM {
        return CountryListUM.Loading(searchBarUM = prevState.searchBarUM, items = loadingItems)
    }
}