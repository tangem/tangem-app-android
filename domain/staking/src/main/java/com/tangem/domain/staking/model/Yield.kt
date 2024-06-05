package com.tangem.domain.staking.model

import java.math.BigDecimal

data class Yield(
    val id: String,
    val token: Token,
    val tokens: List<Token>,
    val args: Args,
    val status: Status,
    val apy: BigDecimal,
    val rewardRate: Double,
    val rewardType: RewardType,
    val metadata: Metadata,
    val validators: List<Validator>,
    val isAvailable: Boolean,
) {
    data class Status(
        val enter: Boolean,
        val exit: Boolean?,
    )

    data class Args(
        val enter: Enter,
        val exit: Enter?,
    ) {
        data class Enter(
            val addresses: Addresses,
            val args: Map<String, AddressArgument>,
        ) {
            data class Addresses(
                val address: AddressArgument,
                val additionalAddresses: Map<String, AddressArgument>? = null,
            )
        }
    }

    data class Validator(
        val address: String,
        val status: String,
        val name: String,
        val image: String?,
        val website: String?,
        val apr: Double?,
        val commission: Double?,
        val stakedBalance: String?,
        val votingPower: Double?,
        val preferred: Boolean,
    )

    data class Metadata(
        val name: String,
        val logoUri: String,
        val description: String,
        val documentation: String?,
        val gasFeeToken: Token,
        val token: Token,
        val tokens: List<Token>,
        val type: String,
        val rewardSchedule: String,
        val cooldownPeriod: Period,
        val warmupPeriod: Period,
        val rewardClaiming: String,
        val defaultValidator: String?,
        val minimumStake: Int?,
        val supportsMultipleValidators: Boolean,
        val revshare: Enabled,
        val fee: Enabled,
    ) {
        data class Period(
            val days: Int,
        )

        data class Enabled(
            val enabled: Boolean,
        )
    }

    enum class RewardType {
        APY, // compound rate
        APR, // simple rate
        UNKNOWN,
    }
}

data class Token(
    val name: String,
    val network: NetworkType,
    val symbol: String,
    val decimals: Int,
    val address: String?,
    val coinGeckoId: String?,
    val logoURI: String?,
    val isPoints: Boolean?,
) {
    enum class NetworkType {
        AVALANCHE_C,
        AVALANCHE_ATOMIC,
        AVALANCHE_P,
        ARBITRUM,
        BINANCE,
        CELO,
        ETHEREUM,
        ETHEREUM_GOERLI,
        ETHEREUM_HOLESKY,
        FANTOM,
        HARMONY,
        OPTIMISM,
        POLYGON,
        GNOSIS,
        MOONRIVER,
        OKC,
        ZKSYNC,
        VICTION,
        AGORIC,
        AKASH,
        AXELAR,
        BAND_PROTOCOL,
        BITSONG,
        CANTO,
        CHIHUAHUA,
        COMDEX,
        COREUM,
        COSMOS,
        CRESCENT,
        CRONOS,
        CUDOS,
        DESMOS,
        DYDX,
        EVMOS,
        FETCH_AI,
        GRAVITY_BRIDGE,
        INJECTIVE,
        IRISNET,
        JUNO,
        KAVA,
        KI_NETWORK,
        MARS_PROTOCOL,
        NYM,
        OKEX_CHAIN,
        ONOMY,
        OSMOSIS,
        PERSISTENCE,
        QUICKSILVER,
        REGEN,
        SECRET,
        SENTINEL,
        SOMMELIER,
        STAFI,
        STARGAZE,
        STRIDE,
        TERITORI,
        TGRADE,
        UMEE,
        POLKADOT,
        KUSAMA,
        WESTEND,
        BINANCEBEACON,
        NEAR,
        SOLANA,
        TEZOS,
        TRON,
        UNKNOWN,
    }
}

data class AddressArgument(
    val required: Boolean,
    val network: String? = null,
    val minimum: Double? = null,
    val maximum: Double? = null,
)