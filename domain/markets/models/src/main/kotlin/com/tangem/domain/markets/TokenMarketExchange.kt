package com.tangem.domain.markets

import java.math.BigDecimal

/**
 * Token exchange
 *
 * @property id            id
 * @property name          name
 * @property imageUrl      image url
 * @property isCentralized CEX (true), DEX (false)
 * @property volumeInUsd   aggregated volume in USD
 * @property trustScore    trust score
 *
[REDACTED_AUTHOR]
 */
data class TokenMarketExchange(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val isCentralized: Boolean,
    val volumeInUsd: BigDecimal,
    val trustScore: TrustScore,
) {

    enum class TrustScore {
        Risky,
        Caution,
        Trusted,
    }
}