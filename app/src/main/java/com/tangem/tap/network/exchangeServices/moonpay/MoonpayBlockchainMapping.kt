package com.tangem.tap.network.exchangeServices.moonpay

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Blockchain.*
import com.tangem.tap.network.exchangeServices.moonpay.models.MoonPaySupportedCurrency

/**
 * Map [Blockchain] to [MoonPaySupportedCurrency.networkCode] and [MoonPaySupportedCurrency.currencyCode]
 * from [link](https://api.moonpay.com/v3/currencies/)
 */
internal val Blockchain.moonPaySupportedCurrency: MoonPaySupportedCurrency?
    get() = when (this) {
        Algorand -> MoonPaySupportedCurrency(networkCode = "algorand", currencyCode = "algo")
        Aptos -> MoonPaySupportedCurrency(networkCode = "aptos", currencyCode = "apt")
        Arbitrum -> MoonPaySupportedCurrency(networkCode = "arbitrum", currencyCode = "eth_arbitrum")
        Avalanche -> MoonPaySupportedCurrency(networkCode = "avalanche_c_chain", currencyCode = "avax_cchain")
        Binance -> MoonPaySupportedCurrency(networkCode = "bnb_chain", currencyCode = "bnb")
        Bitcoin -> MoonPaySupportedCurrency(networkCode = "bitcoin", currencyCode = "btc")
        BitcoinCash -> MoonPaySupportedCurrency(networkCode = "bitcoin_cash", currencyCode = "bch")
        BSC -> MoonPaySupportedCurrency(networkCode = "binance_smart_chain", currencyCode = "bnb_bsc")
        Cardano -> MoonPaySupportedCurrency(networkCode = "cardano", currencyCode = "ada")
        Cosmos -> MoonPaySupportedCurrency(networkCode = "cosmos", currencyCode = "atom")
        Dogecoin -> MoonPaySupportedCurrency(networkCode = "dogecoin", currencyCode = "doge")
        Ethereum -> MoonPaySupportedCurrency(networkCode = "ethereum", currencyCode = "eth")
        EthereumClassic -> MoonPaySupportedCurrency(networkCode = "ethereum_classic", currencyCode = "etc")
        Hedera -> MoonPaySupportedCurrency(networkCode = "hedera", currencyCode = "hbar")
        Litecoin -> MoonPaySupportedCurrency(networkCode = "litecoin", currencyCode = "ltc")
        Near -> MoonPaySupportedCurrency(networkCode = "near", currencyCode = "near")
        Optimism -> MoonPaySupportedCurrency(networkCode = "optimism", currencyCode = "eth_optimism")
        Polkadot -> MoonPaySupportedCurrency(networkCode = "polkadot", currencyCode = "dot")
        Polygon -> MoonPaySupportedCurrency(networkCode = "polygon", currencyCode = "matic_polygon")
        Ravencoin -> MoonPaySupportedCurrency(networkCode = "ravencoin", currencyCode = "rvn")
        Solana -> MoonPaySupportedCurrency(networkCode = "solana", currencyCode = "sol")
        Stellar -> MoonPaySupportedCurrency(networkCode = "stellar", currencyCode = "xlm")
        Tezos -> MoonPaySupportedCurrency(networkCode = "tezos", currencyCode = "xtz")
        TON -> MoonPaySupportedCurrency(networkCode = "ton", currencyCode = "ton")
        Tron -> MoonPaySupportedCurrency(networkCode = "tron", currencyCode = "trx")
        VeChain -> MoonPaySupportedCurrency(networkCode = "vechain", currencyCode = "vet")
        XRP -> MoonPaySupportedCurrency(networkCode = "ripple", currencyCode = "xrp")
        Sei -> MoonPaySupportedCurrency(networkCode = "sei", currencyCode = "sei_sei")
        ZkSyncEra -> MoonPaySupportedCurrency(networkCode = "zksync", currencyCode = "eth_zksync")
        Base -> MoonPaySupportedCurrency(networkCode = "base", currencyCode = "eth_base")
        Filecoin -> MoonPaySupportedCurrency(networkCode = "filecoin", currencyCode = "fil")
        Sui -> MoonPaySupportedCurrency(networkCode = "sui", currencyCode = "sui")
        Core -> MoonPaySupportedCurrency(networkCode = "core", currencyCode = "core")
        Chiliz -> MoonPaySupportedCurrency(networkCode = "ethereum", currencyCode = "chz")
        ArbitrumTestnet -> null
        AvalancheTestnet -> null
        BinanceTestnet -> null
        BSCTestnet -> null
        BitcoinTestnet -> null
        BitcoinCashTestnet -> null
        CosmosTestnet -> null
        Ducatus -> null
        EthereumTestnet -> null
        EthereumClassicTestnet -> null
        Fantom -> null
        FantomTestnet -> null
        NearTestnet -> null
        PolkadotTestnet -> null
        Kava -> null // doesn't support KavaEvm
        KavaTestnet -> null
        Kusama -> null
        PolygonTestnet -> null
        RSK -> null
        SeiTestnet -> null
        StellarTestnet -> null
        SolanaTestnet -> null
        TronTestnet -> null
        Gnosis -> null
        Dash -> null
        OptimismTestnet -> null
        Dischain -> null
        EthereumPow -> null
        EthereumPowTestnet -> null
        Kaspa -> null
        Telos -> null
        TelosTestnet -> null
        TONTestnet -> null
        RavencoinTestnet -> null
        TerraV1 -> null
        TerraV2 -> null
        Cronos -> null
        AlephZero -> null
        AlephZeroTestnet -> null
        OctaSpace -> null
        OctaSpaceTestnet -> null
        Chia -> null
        ChiaTestnet -> null
        Decimal -> null
        DecimalTestnet -> null
        XDC -> null
        XDCTestnet -> null
        VeChainTestnet -> null
        AptosTestnet -> null
        Playa3ull -> null
        Shibarium -> null
        ShibariumTestnet -> null
        AlgorandTestnet -> null
        HederaTestnet -> null
        Aurora -> null
        AuroraTestnet -> null
        Areon -> null
        AreonTestnet -> null
        PulseChain -> null
        PulseChainTestnet -> null
        ZkSyncEraTestnet -> null
        Nexa -> null
        NexaTestnet -> null
        Moonbeam -> null
        MoonbeamTestnet -> null
        Manta -> null
        MantaTestnet -> null
        PolygonZkEVM -> null
        PolygonZkEVMTestnet -> null
        Radiant -> null
        Fact0rn -> null
        BaseTestnet -> null
        Moonriver -> null
        MoonriverTestnet -> null
        Mantle -> null
        MantleTestnet -> null
        Flare -> null
        FlareTestnet -> null
        Taraxa -> null
        TaraxaTestnet -> null
        Koinos -> null
        KoinosTestnet -> null
        Joystream -> null
        Bittensor -> null
        Blast -> null
        BlastTestnet -> null
        Cyber -> null
        CyberTestnet -> null
        InternetComputer -> null
        SuiTestnet -> null
        EnergyWebChain -> null
        EnergyWebChainTestnet -> null
        EnergyWebX -> null
        EnergyWebXTestnet -> null
        Casper -> null
        CasperTestnet -> null
        CoreTestnet -> null
        ChilizTestnet -> null
        Unknown -> null
        Xodex -> null
        Canxium -> null
        Clore -> null
        VanarChain -> null
        VanarChainTestnet -> null
    }