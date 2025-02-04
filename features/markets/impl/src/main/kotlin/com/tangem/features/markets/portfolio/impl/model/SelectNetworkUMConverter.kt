package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.portfolio.impl.ui.state.SelectNetworkUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter from [TokenMarketParams] to [SelectNetworkUM]
 *
 * @property networksWithToggle   map of networks with toggles
 * @property alreadyAddedNetworks already added networks
 * @property onNetworkSwitchClick callback is called when network switch is clicked
 *
[REDACTED_AUTHOR]
 */
internal class SelectNetworkUMConverter(
    private val networksWithToggle: Map<TokenMarketInfo.Network, Boolean>,
    private val alreadyAddedNetworks: Set<String>,
    private val onNetworkSwitchClick: (BlockchainRowUM, Boolean) -> Unit,
) : Converter<TokenMarketParams, SelectNetworkUM> {

    override fun convert(value: TokenMarketParams): SelectNetworkUM {
        return SelectNetworkUM(
            tokenId = value.id.value,
            iconUrl = value.imageUrl,
            tokenName = value.name,
            tokenCurrencySymbol = value.symbol,
            networks = BlockchainRowUMConverter(alreadyAddedNetworks)
                .convertList(networksWithToggle.toList())
                .toImmutableList(),
            onNetworkSwitchClick = { um, isChecked -> onNetworkSwitchClick(um, isChecked) },
        )
    }
}