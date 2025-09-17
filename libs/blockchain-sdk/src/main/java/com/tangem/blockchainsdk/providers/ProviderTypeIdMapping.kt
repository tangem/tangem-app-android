package com.tangem.blockchainsdk.providers

import com.tangem.blockchain.common.network.providers.ProviderType

/** Mapping of [providerType] with [id] */
internal enum class ProviderTypeIdMapping(val id: String, val providerType: ProviderType) {
    NowNodes(id = "nownodes", providerType = ProviderType.NowNodes),
    GetBlock(id = "getblock", providerType = ProviderType.GetBlock),
    QuickNode(id = "quicknode", providerType = ProviderType.QuickNode),
    BitcoinBlockchair(id = "blockchair", providerType = ProviderType.BitcoinLike.Blockchair),
    BitcoinBlockcypher(id = "blockcypher", providerType = ProviderType.BitcoinLike.Blockcypher),
    CardanoAdalite(id = "adalite", providerType = ProviderType.Cardano.Adalite),
    CardanoRosetta(id = "tangemRosetta", providerType = ProviderType.Cardano.Rosetta),
    ChiaFireAcademy(id = "fireAcademy", providerType = ProviderType.Chia.FireAcademy),
    ChiaTangem(id = "tangemChia", providerType = ProviderType.Chia.Tangem),
    ChiaTangemNew(id = "tangemChia3", providerType = ProviderType.Chia.TangemNew),
    EthereumInfura(id = "infura", providerType = ProviderType.EthereumLike.Infura),
    HederaArkhia(id = "arkhiaHedera", providerType = ProviderType.Hedera.Arkhia),
    KaspaSecondary(id = "kaspa", providerType = ProviderType.Kaspa.SecondaryAPI),
    SolanaOfficial(id = "solana", providerType = ProviderType.Solana.Official),
    TonCentral(id = "ton", providerType = ProviderType.Ton.TonCentral),
    TronGrid(id = "tron", providerType = ProviderType.Tron.TronGrid),
    BittensorDwellir(id = "dwellirBittensor", providerType = ProviderType.Bittensor.Dwellir),
    BittensorOnfinality(id = "onfinalityBittensor", providerType = ProviderType.Bittensor.Onfinality),
    KoinosPro(id = "koinospro", providerType = ProviderType.Koinos.KoinosPro),
    AlephiumTangem(id = "tangemAlephium", providerType = ProviderType.Alephium.Tangem),
}