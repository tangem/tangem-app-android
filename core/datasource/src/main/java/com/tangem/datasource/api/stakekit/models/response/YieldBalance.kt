package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldBalances(
    @Json(name = "balances")
    val balances: List<Balance>,
    @Json(name = "integrationId")
    val integrationId: String,
)

@JsonClass(generateAdapter = true)
data class Balance(
    @Json(name = "groupId")
    val groupId: String,
    @Json(name = "type")
    val type: BalanceType,
    @Json(name = "amount")
    val amount: BigDecimal,
    @Json(name = "date")
    val date: DateTime?,
    @Json(name = "pricePerShare")
    val pricePerShare: BigDecimal,
    @Json(name = "pendingActions")
    val pendingActions: List<PendingAction>,
    @Json(name = "token")
    val token: Token,
    @Json(name = "validatorAddress")
    val validatorAddress: String?,
    @Json(name = "validatorAddresses")
    val validatorAddresses: List<String>?,
    @Json(name = "providerId")
    val providerId: String?,
)

@JsonClass(generateAdapter = true)
data class PendingAction(
    @Json(name = "type")
    val type: ActionType,
    @Json(name = "passthrough")
    val passthrough: String,
    @Json(name = "args")
    val args: PendingActionArgs?,
)

@JsonClass(generateAdapter = true)
data class PendingActionArgs(
    @Json(name = "amount")
    val amount: Amount?,
    @Json(name = "duration")
    val duration: Duration?,
    @Json(name = "validatorAddress")
    val validatorAddress: ValidatorAddress?,
    @Json(name = "validatorAddresses")
    val validatorAddresses: ValidatorAddresses?,
    @Json(name = "nfts")
    val nfts: List<Nft>?,
    @Json(name = "tronResource")
    val tronResource: TronResource?,
    @Json(name = "signatureVerification")
    val signatureVerification: SignatureVerification?,
)

@JsonClass(generateAdapter = true)
data class Amount(
    @Json(name = "required")
    val required: Boolean,
    @Json(name = "minimum")
    val minimum: Double?,
    @Json(name = "maximum")
    val maximum: Double?,
)

@JsonClass(generateAdapter = true)
data class Duration(
    @Json(name = "required")
    val required: Boolean,
    @Json(name = "minimum")
    val minimum: Int?,
    @Json(name = "maximum")
    val maximum: Int?,
)

@JsonClass(generateAdapter = true)
data class ValidatorAddress(
    @Json(name = "required")
    val required: Boolean,
)

@JsonClass(generateAdapter = true)
data class ValidatorAddresses(
    @Json(name = "required")
    val required: Boolean,
)

@JsonClass(generateAdapter = true)
data class Nft(
    @Json(name = "baycId")
    val baycId: BaycId?,
    @Json(name = "maycId")
    val maycId: MaycId?,
    @Json(name = "bakcId")
    val bakcId: BakcId?,
)

@JsonClass(generateAdapter = true)
data class BaycId(
    @Json(name = "required")
    val required: Boolean,
)

@JsonClass(generateAdapter = true)
data class MaycId(
    @Json(name = "required")
    val required: Boolean,
)

@JsonClass(generateAdapter = true)
data class BakcId(
    @Json(name = "required")
    val required: Boolean,
)

@JsonClass(generateAdapter = true)
data class TronResource(
    @Json(name = "required")
    val required: Boolean,
)

@JsonClass(generateAdapter = true)
data class SignatureVerification(
    @Json(name = "required")
    val required: Boolean,
)

enum class BalanceType {
    @Json(name = "available")
    AVAILABLE,

    @Json(name = "staked")
    STAKED,

    @Json(name = "unstaking")
    UNSTAKING,

    @Json(name = "unstaked")
    UNSTAKED,

    @Json(name = "preparing")
    PREPARING,

    @Json(name = "rewards")
    REWARDS,

    @Json(name = "locked")
    LOCKED,

    @Json(name = "unlocking")
    UNLOCKING,
}

enum class ActionType {
    @Json(name = "STAKE")
    STAKE,

    @Json(name = "UNSTAKE")
    UNSTAKE,

    @Json(name = "CLAIM_REWARDS")
    CLAIM_REWARDS,

    @Json(name = "RESTAKE_REWARDS")
    RESTAKE_REWARDS,

    @Json(name = "WITHDRAW")
    WITHDRAW,

    @Json(name = "RESTAKE")
    RESTAKE,

    @Json(name = "CLAIM_UNSTAKED")
    CLAIM_UNSTAKED,

    @Json(name = "UNLOCK_LOCKED")
    UNLOCK_LOCKED,

    @Json(name = "STAKE_LOCKED")
    STAKE_LOCKED,

    @Json(name = "VOTE")
    VOTE,

    @Json(name = "REVOKE")
    REVOKE,

    @Json(name = "VOTE_LOCKED")
    VOTE_LOCKED,

    @Json(name = "REVOTE")
    REVOTE,

    @Json(name = "REBOND")
    REBOND,

    @Json(name = "MIGRATE")
    MIGRATE,
}

enum class NetworkType {
    @Json(name = "avalanche")
    AVALANCHE,

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
}
