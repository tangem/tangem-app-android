package com.tangem.features.onramp.swap.availablepairs.entity.transformers

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.swap.availablepairs.entity.converters.LoadingTokenListItemConverter
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Set [statuses] as loading items
 *
[REDACTED_AUTHOR]
 */
internal class SetLoadingTokenItemsTransformer(
    private val statuses: List<CryptoCurrencyStatus>,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        return prevState.copy(
            availableItems = LoadingTokenListItemConverter.convertList(input = statuses).toImmutableList(),
            unavailableItems = persistentListOf(),
            warning = null,
        )
    }
}