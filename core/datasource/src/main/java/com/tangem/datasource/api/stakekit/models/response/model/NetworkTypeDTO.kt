package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json

enum class NetworkTypeDTO {
    @Json(name = "avalanche-c")
    AVALANCHE_C,

    @Json(name = "avalanche-atomic")
    AVALANCHE_ATOMIC,

    @Json(name = "avalanche-p")
    AVALANCHE_P,

    @Json(name = "arbitrum")
    ARBITRUM,

    @Json(name = "binance")
    BINANCE,

    @Json(name = "celo")
    CELO,

    @Json(name = "ethereum")
    ETHEREUM,

    @Json(name = "ethereum-goerli")
    ETHEREUM_GOERLI,

    @Json(name = "ethereum-holesky")
    ETHEREUM_HOLESKY,

    @Json(name = "fantom")
    FANTOM,

    @Json(name = "harmony")
    HARMONY,

    @Json(name = "optimism")
    OPTIMISM,

    @Json(name = "polygon")
    POLYGON,

    @Json(name = "gnosis")
    GNOSIS,

    @Json(name = "moonriver")
    MOONRIVER,

    @Json(name = "okc")
    OKC,

    @Json(name = "zksync")
    ZKSYNC,

    @Json(name = "viction")
    VICTION,

    @Json(name = "agoric")
    AGORIC,

    @Json(name = "akash")
    AKASH,

    @Json(name = "axelar")
    AXELAR,

    @Json(name = "band-protocol")
    BAND_PROTOCOL,

    @Json(name = "bitsong")
    BITSONG,

    @Json(name = "canto")
    CANTO,

    @Json(name = "chihuahua")
    CHIHUAHUA,

    @Json(name = "comdex")
    COMDEX,

    @Json(name = "coreum")
    COREUM,

    @Json(name = "cosmos")
    COSMOS,

    @Json(name = "crescent")
    CRESCENT,

    @Json(name = "cronos")
    CRONOS,

    @Json(name = "cudos")
    CUDOS,

    @Json(name = "desmos")
    DESMOS,

    @Json(name = "dydx")
    DYDX,

    @Json(name = "evmos")
    EVMOS,

    @Json(name = "fetch-ai")
    FETCH_AI,

    @Json(name = "gravity-bridge")
    GRAVITY_BRIDGE,

    @Json(name = "injective")
    INJECTIVE,

    @Json(name = "irisnet")
    IRISNET,

    @Json(name = "juno")
    JUNO,

    @Json(name = "kava")
    KAVA,

    @Json(name = "ki-network")
    KI_NETWORK,

    @Json(name = "mars-protocol")
    MARS_PROTOCOL,

    @Json(name = "nym")
    NYM,

    @Json(name = "okex-chain")
    OKEX_CHAIN,

    @Json(name = "onomy")
    ONOMY,

    @Json(name = "osmosis")
    OSMOSIS,

    @Json(name = "persistence")
    PERSISTENCE,

    @Json(name = "quicksilver")
    QUICKSILVER,

    @Json(name = "regen")
    REGEN,

    @Json(name = "secret")
    SECRET,

    @Json(name = "sentinel")
    SENTINEL,

    @Json(name = "sommelier")
    SOMMELIER,

    @Json(name = "stafi")
    STAFI,

    @Json(name = "stargaze")
    STARGAZE,

    @Json(name = "stride")
    STRIDE,

    @Json(name = "teritori")
    TERITORI,

    @Json(name = "tgrade")
    TGRADE,

    @Json(name = "umee")
    UMEE,

    @Json(name = "polkadot")
    POLKADOT,

    @Json(name = "kusama")
    KUSAMA,

    @Json(name = "westend")
    WESTEND,

    @Json(name = "binancebeacon")
    BINANCEBEACON,

    @Json(name = "near")
    NEAR,

    @Json(name = "solana")
    SOLANA,

    @Json(name = "tezos")
    TEZOS,

    @Json(name = "tron")
    TRON,

    UNKNOWN,
}