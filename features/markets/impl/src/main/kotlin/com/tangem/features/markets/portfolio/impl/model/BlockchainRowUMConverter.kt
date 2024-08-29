package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.converter.Converter

/**
[REDACTED_AUTHOR]
 */
internal object BlockchainRowUMConverter : Converter<Pair<TokenMarketInfo.Network, Boolean>, BlockchainRowUM> {

    override fun convert(value: Pair<TokenMarketInfo.Network, Boolean>): BlockchainRowUM {
        val (network, isSelected) = value

        val blockchainInfo = BlockchainUtils.getNetworkInfo(networkId = network.networkId)

        val isMainNetwork = network.contractAddress == null

        return BlockchainRowUM(
            id = network.networkId,
            name = blockchainInfo.name,
            type = if (isMainNetwork) "MAIN" else blockchainInfo.protocolName,
            iconResId = if (isSelected) {
                getActiveIconRes(blockchainInfo.blockchainId)
            } else {
                getGreyedOutIconRes(blockchainInfo.blockchainId)
            },
            isMainNetwork = isMainNetwork,
            isSelected = isSelected,
        )
    }
}