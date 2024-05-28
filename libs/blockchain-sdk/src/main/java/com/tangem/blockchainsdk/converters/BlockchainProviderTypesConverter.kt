package com.tangem.blockchainsdk.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.blockchainsdk.BlockchainProviderTypes
import com.tangem.blockchainsdk.BlockchainProvidersResponse
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.config.models.ProviderModel
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
                    is ProviderModel.Private -> createPrivateProviderType(blockchain = blockchain, name = provider.name)
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

    @Suppress("CyclomaticComplexMethod")
    private fun createPrivateProviderType(blockchain: Blockchain, name: String): ProviderType? {
        return when (name) {
            "blockchair" -> ProviderType.BitcoinLike.Blockchair
            "blockcypher" -> ProviderType.BitcoinLike.Blockcypher
            "adalite" -> ProviderType.Cardano.Adalite
            "tangemRosetta" -> ProviderType.Cardano.Rosetta
            "fireAcademy" -> ProviderType.Chia.FireAcademy
            "tangemChia" -> ProviderType.Chia.Tangem
            "infura" -> ProviderType.EthereumLike.Infura
            "getblock" -> ProviderType.GetBlock
            "arkhiaHedera" -> ProviderType.Hedera.Arkhia
            "kaspa" -> ProviderType.Kaspa.SecondaryAPI
            "nownodes" -> ProviderType.NowNodes
            "quicknode" -> ProviderType.QuickNode
            "solana" -> ProviderType.Solana.Official
            "ton" -> ProviderType.Ton.TonCentral
            "tron" -> ProviderType.Tron.TronGrid
            else -> {
                Timber.e("$blockchain private provider ($name) is not supported")
                null
            }
        }
    }
}