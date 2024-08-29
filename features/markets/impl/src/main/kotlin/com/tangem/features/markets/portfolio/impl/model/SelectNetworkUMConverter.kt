package com.tangem.features.markets.portfolio.impl.model

import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.portfolio.impl.ui.state.SelectNetworkUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
[REDACTED_AUTHOR]
 */
internal class SelectNetworkUMConverter(
    private val networksWithToggle: Map<TokenMarketInfo.Network, Boolean>,
    private val onNetworkSwitchClick: (String, Boolean) -> Unit,
) : Converter<TokenMarketParams, SelectNetworkUM> {

    override fun convert(value: TokenMarketParams): SelectNetworkUM {
        return SelectNetworkUM(
            tokenId = value.id,
            iconUrl = value.imageUrl,
            tokenName = value.name,
            tokenCurrencySymbol = value.symbol,
            networks = BlockchainRowUMConverter.convertList(networksWithToggle.toList()).toImmutableList(),
            onNetworkSwitchClick = { um, isChecked -> onNetworkSwitchClick(um.id, isChecked) },
        )
    }
}