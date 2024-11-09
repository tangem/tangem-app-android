package com.tangem.features.onramp.swap.availablepairs.entity.transformers

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.onramp.swap.availablepairs.entity.converters.LoadingTokenListItemConverter
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.toImmutableList

/**
 * Set [statuses] as loading items
 *
 * @author Andrew Khokhlov on 09/11/2024
 */
internal class SetLoadingTokenItemsTransformer(
    private val statuses: List<CryptoCurrencyStatus>,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        return prevState.copy(
            availableItems = LoadingTokenListItemConverter.convertList(input = statuses).toImmutableList(),
        )
    }
}
