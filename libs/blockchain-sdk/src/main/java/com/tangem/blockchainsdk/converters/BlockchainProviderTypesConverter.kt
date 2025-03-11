package com.tangem.blockchainsdk.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.providers.BlockchainProviderTypes
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.utils.converter.TwoWayConverter
import timber.log.Timber

/**
 * Converts [BlockchainProvidersResponse] to [BlockchainProviderTypes] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object BlockchainProviderTypesConverter :
    TwoWayConverter<BlockchainProvidersResponse, BlockchainProviderTypes> {

    override fun convert(value: BlockchainProvidersResponse): BlockchainProviderTypes {
        return value.mapNotNull { (networkId, blockchainProviders) ->
            val blockchain = Blockchain.fromNetworkId(networkId) ?: return@mapNotNull null

            val providerTypes = ProviderTypeConverter.convertList(input = blockchainProviders)

            providerTypes.forEach {
                if (it == null) Timber.e("$blockchain provider type is not supported")
            }

            blockchain to providerTypes.filterNotNull()
        }
            .toMap()
    }

    override fun convertBack(value: BlockchainProviderTypes): BlockchainProvidersResponse {
        return value.mapNotNull { (blockchain, providerTypes) ->
            val networkId = blockchain.toNetworkId()

            val blockchainProviders = ProviderTypeConverter.convertListBack(input = providerTypes)

            networkId to blockchainProviders
        }
            .toMap()
    }
}