package com.tangem.features.onramp.selectcurrency.entity.transformer

import arrow.core.Either
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.model.OnrampCurrencies
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesSection
import com.tangem.features.onramp.selectcurrency.entity.CurrencyItemState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class UpdateCurrencyItemsTransformer(
    private val maybeCurrencies: Either<OnrampError, OnrampCurrencies>,
    private val query: String,
    private val onRetry: () -> Unit,
    private val onCurrencyClick: (OnrampCurrency) -> Unit,
) : Transformer<CurrenciesListUM> {

    override fun transform(prevState: CurrenciesListUM): CurrenciesListUM {
        return maybeCurrencies.fold(
            ifLeft = { CurrenciesListUM.Error(searchBarUM = prevState.searchBarUM, onRetry = onRetry) },
            ifRight = { currencies ->
                val populars = currencies.populars.filterByQuery()
                val others = currencies.others.filterByQuery()
                val sections = buildList {
                    if (populars.isNotEmpty()) {
                        add(
                            CurrenciesSection(
                                title = resourceReference(R.string.onramp_currency_popular),
                                items = populars.map(::convertCurrencyToUiModel).toImmutableList(),
                            ),
                        )
                    }
                    if (others.isNotEmpty()) {
                        add(
                            CurrenciesSection(
                                title = resourceReference(R.string.onramp_currency_other),
                                items = others.map(::convertCurrencyToUiModel).toImmutableList(),
                            ),
                        )
                    }
                }
                CurrenciesListUM.Content(
                    searchBarUM = prevState.searchBarUM,
                    sections = sections.toImmutableList(),
                )
            },
        )
    }

    private fun List<OnrampCurrency>.filterByQuery(): List<OnrampCurrency> {
        return filter {
            it.code.lowercase().contains(query.lowercase()) || it.name.lowercase().contains(query.lowercase())
        }
    }

    private fun convertCurrencyToUiModel(onrampCurrency: OnrampCurrency): CurrencyItemState.Content {
        return CurrencyItemState.Content(
            id = onrampCurrency.code,
            onrampCurrency = onrampCurrency,
            onClick = { onCurrencyClick(onrampCurrency) },
        )
    }
}