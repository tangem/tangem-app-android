package com.tangem.domain.markets

import java.math.BigDecimal

/**
 * Global market indicators of the "Total market cap" block
 *
 * @property totalMarketCap     total capitalisation, already converted by the backend into the currency
 *                              the request asked for; `null` when the backend has no value
 * @property marketCapChange24h 24h capitalisation change as a **fraction** (`-0.0161` for -1.61%),
 *                              matching [MarketCategory]; `null` when the backend has no value
 * @property fearGreed          Fear & Greed index; `null` when the backend has no value
 * @property altcoinSeason      Altcoin Season index; `null` when the backend has no value
 */
data class MarketIndicators(
    val totalMarketCap: BigDecimal?,
    val marketCapChange24h: BigDecimal?,
    val fearGreed: Index?,
    val altcoinSeason: Index?,
) {

    /**
     * A market index reading
     *
     * @property value          position on the [SCALE]
     * @property classification label of the band [value] falls into, e.g. `Fear`, `Altcoin Season`.
     *                          Localised by the backend, so it is displayed as received and never parsed
     */
    data class Index(
        val value: Int,
        val classification: String,
    ) {

        companion object {

            /** Both indices are published on the same scale */
            val SCALE = 0..100
        }
    }
}