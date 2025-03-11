package com.tangem.features.onramp.selectcurrency.entity.transformer

import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesSection
import com.tangem.features.onramp.selectcurrency.entity.CurrencyItemState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

internal class UpdateCurrencyItemsLoadingTransformer(
    private val loadingSections: ImmutableList<CurrenciesSection<CurrencyItemState.Loading>>,
) : Transformer<CurrenciesListUM> {

    override fun transform(prevState: CurrenciesListUM): CurrenciesListUM {
        return CurrenciesListUM.Loading(searchBarUM = prevState.searchBarUM, sections = loadingSections)
    }
}