package com.tangem.grow.datasource.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = false)
enum class NetworkTypeDTO {
    @SerialName("avalanche-c")
    @Json(name = "avalanche-c")
    AVALANCHE_C,

    @SerialName("avalanche-atomic")
    @Json(name = "avalanche-atomic")
    AVALANCHE_ATOMIC,

    @SerialName("avalanche-p")
    @Json(name = "avalanche-p")
    AVALANCHE_P,

    @SerialName("arbitrum")
    @Json(name = "arbitrum")
    ARBITRUM,

    @SerialName("binance")
    @Json(name = "binance")
    BINANCE,

    @SerialName("celo")
    @Json(name = "celo")
    CELO,

    @SerialName("ethereum")
    @Json(name = "ethereum")
    ETHEREUM,

    @SerialName("ethereum-goerli")
    @Json(name = "ethereum-goerli")
    ETHEREUM_GOERLI,

    @SerialName("ethereum-holesky")
    @Json(name = "ethereum-holesky")
    ETHEREUM_HOLESKY,

    @SerialName("fantom")
    @Json(name = "fantom")
    FANTOM,

    @SerialName("harmony")
    @Json(name = "harmony")
    HARMONY,

    @SerialName("optimism")
    @Json(name = "optimism")
    OPTIMISM,

    @SerialName("polygon")
    @Json(name = "polygon")
    POLYGON,

    @SerialName("gnosis")
    @Json(name = "gnosis")
    GNOSIS,

    @SerialName("moonriver")
    @Json(name = "moonriver")
    MOONRIVER,

    @SerialName("okc")
    @Json(name = "okc")
    OKC,

    @SerialName("zksync")
    @Json(name = "zksync")
    ZKSYNC,

    @SerialName("viction")
    @Json(name = "viction")
    VICTION,

    @SerialName("agoric")
    @Json(name = "agoric")
    AGORIC,

    @SerialName("akash")
    @Json(name = "akash")
    AKASH,

    @SerialName("axelar")
    @Json(name = "axelar")
    AXELAR,

    @SerialName("band-protocol")
    @Json(name = "band-protocol")
    BAND_PROTOCOL,

    @SerialName("bitsong")
    @Json(name = "bitsong")
    BITSONG,

    @SerialName("canto")
    @Json(name = "canto")
    CANTO,

    @SerialName("cardano")
    @Json(name = "cardano")
    CARDANO,

    @SerialName("chihuahua")
    @Json(name = "chihuahua")
    CHIHUAHUA,

    @SerialName("comdex")
    @Json(name = "comdex")
    COMDEX,

    @SerialName("coreum")
    @Json(name = "coreum")
    COREUM,

    @SerialName("cosmos")
    @Json(name = "cosmos")
    COSMOS,

    @SerialName("crescent")
    @Json(name = "crescent")
    CRESCENT,

    @SerialName("cronos")
    @Json(name = "cronos")
    CRONOS,

    @SerialName("cudos")
    @Json(name = "cudos")
    CUDOS,

    @SerialName("desmos")
    @Json(name = "desmos")
    DESMOS,

    @SerialName("dydx")
    @Json(name = "dydx")
    DYDX,

    @SerialName("evmos")
    @Json(name = "evmos")
    EVMOS,

    @SerialName("fetch-ai")
    @Json(name = "fetch-ai")
    FETCH_AI,

    @SerialName("gravity-bridge")
    @Json(name = "gravity-bridge")
    GRAVITY_BRIDGE,

    @SerialName("injective")
    @Json(name = "injective")
    INJECTIVE,

    @SerialName("irisnet")
    @Json(name = "irisnet")
    IRISNET,

    @SerialName("juno")
    @Json(name = "juno")
    JUNO,

    @SerialName("kava")
    @Json(name = "kava")
    KAVA,

    @SerialName("ki-network")
    @Json(name = "ki-network")
    KI_NETWORK,

    @SerialName("mars-protocol")
    @Json(name = "mars-protocol")
    MARS_PROTOCOL,

    @SerialName("nym")
    @Json(name = "nym")
    NYM,

    @SerialName("okex-chain")
    @Json(name = "okex-chain")
    OKEX_CHAIN,

    @SerialName("onomy")
    @Json(name = "onomy")
    ONOMY,

    @SerialName("osmosis")
    @Json(name = "osmosis")
    OSMOSIS,

    @SerialName("persistence")
    @Json(name = "persistence")
    PERSISTENCE,

    @SerialName("quicksilver")
    @Json(name = "quicksilver")
    QUICKSILVER,

    @SerialName("regen")
    @Json(name = "regen")
    REGEN,

    @SerialName("secret")
    @Json(name = "secret")
    SECRET,

    @SerialName("sentinel")
    @Json(name = "sentinel")
    SENTINEL,

    @SerialName("sommelier")
    @Json(name = "sommelier")
    SOMMELIER,

    @SerialName("stafi")
    @Json(name = "stafi")
    STAFI,

    @SerialName("stargaze")
    @Json(name = "stargaze")
    STARGAZE,

    @SerialName("stride")
    @Json(name = "stride")
    STRIDE,

    @SerialName("teritori")
    @Json(name = "teritori")
    TERITORI,

    @SerialName("tgrade")
    @Json(name = "tgrade")
    TGRADE,

    @SerialName("umee")
    @Json(name = "umee")
    UMEE,

    @SerialName("polkadot")
    @Json(name = "polkadot")
    POLKADOT,

    @SerialName("kusama")
    @Json(name = "kusama")
    KUSAMA,

    @SerialName("westend")
    @Json(name = "westend")
    WESTEND,

    @SerialName("binancebeacon")
    @Json(name = "binancebeacon")
    BINANCEBEACON,

    @SerialName("near")
    @Json(name = "near")
    NEAR,

    @SerialName("solana")
    @Json(name = "solana")
    SOLANA,

    @SerialName("tezos")
    @Json(name = "tezos")
    TEZOS,

    @SerialName("tron")
    @Json(name = "tron")
    TRON,

    @SerialName("ton")
    @Json(name = "ton")
    TON,

    UNKNOWN,
}