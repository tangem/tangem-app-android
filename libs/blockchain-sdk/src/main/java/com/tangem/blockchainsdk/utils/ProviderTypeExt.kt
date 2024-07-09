package com.tangem.blockchainsdk.utils

import com.tangem.blockchain.common.network.providers.ProviderType
import timber.log.Timber

/** Create private provider by [name] or return null */
@Suppress("CyclomaticComplexMethod")
fun createPrivateProviderType(name: String): ProviderType? {
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
        "dwellirBittensor" -> ProviderType.Bittensor.Dwellir
        "onfinalityBittensor" -> ProviderType.Bittensor.Onfinality
        "koinospro" -> ProviderType.Koinos.KoinosPro
        else -> {
            Timber.e("Private provider with name $name is not supported")
            null
        }
    }
}