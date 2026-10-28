package com.tangem.domain.polymarket.model

import com.tangem.domain.polymarket.approval.PolymarketContracts

/**
 * The EIP-712 domain an order is signed for: the exchange contract that settles it and the version of that
 * exchange's domain. Only the combinations in [of] exist; an address outside them is never signed for.
 */
data class PolymarketExchangeDomain private constructor(
    val exchange: String,
    val version: String,
) {

    companion object {

        val CTF: PolymarketExchangeDomain = PolymarketExchangeDomain(
            exchange = PolymarketContracts.CTF_EXCHANGE,
            version = PolymarketContracts.EXCHANGE_DOMAIN_VERSION_V2,
        )

        val NEG_RISK_CTF: PolymarketExchangeDomain = PolymarketExchangeDomain(
            exchange = PolymarketContracts.NEG_RISK_CTF_EXCHANGE,
            version = PolymarketContracts.EXCHANGE_DOMAIN_VERSION_V2,
        )

        val EXCHANGE_V3: PolymarketExchangeDomain = PolymarketExchangeDomain(
            exchange = PolymarketContracts.EXCHANGE_V3,
            version = PolymarketContracts.EXCHANGE_DOMAIN_VERSION_V3,
        )

        fun v1(isNegRisk: Boolean): PolymarketExchangeDomain = if (isNegRisk) NEG_RISK_CTF else CTF

        /** @return the only domain a market of [version] settles on, or `null` when the version is unknown */
        fun of(version: PolymarketMarketVersion, isNegRisk: Boolean): PolymarketExchangeDomain? = when (version) {
            PolymarketMarketVersion.V1 -> v1(isNegRisk)
            PolymarketMarketVersion.V2 -> EXCHANGE_V3
            PolymarketMarketVersion.UNKNOWN -> null
        }
    }
}