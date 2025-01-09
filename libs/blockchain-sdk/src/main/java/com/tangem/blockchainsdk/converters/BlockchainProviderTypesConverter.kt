package com.tangem.blockchainsdk.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.providers.BlockchainProviderTypes
import com.tangem.blockchainsdk.utils.createPrivateProviderType
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.local.config.providers.models.ProviderModel
import com.tangem.utils.converter.Converter
import timber.log.Timber

/**
 * Converts [BlockchainProvidersResponse] to [BlockchainProviderTypes]
 *
[REDACTED_AUTHOR]
 */
internal object BlockchainProviderTypesConverter :
    Converter<BlockchainProvidersResponse, BlockchainProviderTypes> {

    override fun convert(value: BlockchainProvidersResponse): BlockchainProviderTypes {
        return value.mapNotNull { (networkId, blockchainProviders) ->
            val blockchain = Blockchain.fromNetworkId(networkId) ?: return@mapNotNull null

            val providerTypes = blockchainProviders.mapNotNull { provider ->
                when (provider) {
                    is ProviderModel.Public -> ProviderType.Public(url = provider.url)
                    is ProviderModel.Private -> createPrivateProviderType(name = provider.name)
                    ProviderModel.UnsupportedType -> {
                        Timber.e("$blockchain provider type is not supported")
                        null
                    }
                }
            }

            blockchain to providerTypes
        }
            .toMap()
    }
}