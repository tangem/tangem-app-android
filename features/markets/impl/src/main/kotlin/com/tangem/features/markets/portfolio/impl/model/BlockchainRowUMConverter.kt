package com.tangem.features.markets.portfolio.impl.model

import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.converter.Converter

/**
 * Converter from [TokenMarketInfo.Network] to [BlockchainRowUM]
 *
 * @property alreadyAddedNetworks set of already added networks
 *
[REDACTED_AUTHOR]
 */
internal class BlockchainRowUMConverter(
    private val alreadyAddedNetworks: Set<String>,
) : Converter<Pair<TokenMarketInfo.Network, Boolean>, BlockchainRowUM> {

    override fun convert(value: Pair<TokenMarketInfo.Network, Boolean>): BlockchainRowUM {
        val (network, isSelected) = value

        val blockchainInfo = BlockchainUtils.getNetworkInfo(networkId = network.networkId)
            ?: error("Can't find blockchain info for ${network.networkId}")

        val isMainNetwork = network.contractAddress == null

        val isEnabled = !alreadyAddedNetworks.contains(network.networkId)

        return BlockchainRowUM(
            id = network.networkId,
            name = blockchainInfo.name,
            type = getNetworkType(network, blockchainInfo),
            iconResId = if (isEnabled) {
                if (isSelected) {
                    getActiveIconRes(blockchainInfo.blockchainId)
                } else {
                    getGreyedOutIconRes(blockchainInfo.blockchainId)
                }
            } else {
                getGreyedOutIconRes(blockchainInfo.blockchainId)
            },
            isMainNetwork = isMainNetwork,
            isSelected = isSelected,
            isEnabled = isEnabled,
        )
    }

    private fun getNetworkType(
        network: TokenMarketInfo.Network,
        blockchainInfo: BlockchainUtils.BlockchainInfo,
    ): String {
        val isMainNetwork = network.contractAddress == null
        return when {
            BlockchainUtils.isL2Network(networkId = network.networkId) -> MAIN_NETWORK_L2_TYPE_NAME
            isMainNetwork -> MAIN_NETWORK_TYPE_NAME
            else -> blockchainInfo.protocolName
        }
    }

    private companion object {
        const val MAIN_NETWORK_TYPE_NAME = "MAIN"
        const val MAIN_NETWORK_L2_TYPE_NAME = "MAIN L2"
    }
}