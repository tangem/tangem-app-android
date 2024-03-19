package com.tangem.tap.network.exchangeServices.moonpay

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Blockchain.*
import com.tangem.tap.network.exchangeServices.moonpay.models.MoonPaySupportedCurrency

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
        else -> null
    }
