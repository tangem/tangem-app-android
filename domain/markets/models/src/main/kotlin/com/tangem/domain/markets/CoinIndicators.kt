package com.tangem.domain.markets

import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * Indicator readings for a single coin
 *
 * @param symbol   coin symbol (e.g. `BTC`)
 * @param readings indicator readings
 */
data class CoinIndicators(
    val symbol: String,
    val readings: List<Reading>,
) {

    /**
     * Single indicator reading for a coin
     *
     * @param type      indicator type
     * @param timeframe reading timeframe for RSI and MACD; `null` for the timeframe-agnostic
     *                  indicators (MA_CROSS, GALAXY_SCORE, SENTIMENT)
     * @param value     RSI value; MACD histogram; galaxy/sentiment score (0–100).
     *                  Always `null` for MA_CROSS and for non-signal states
     * @param signal    interpreted signal
     * @param subLabel  RSI only: `Overbought` / `Oversold`. `null` otherwise
     * @param updatedAt timestamp of the last stored value, or `null`
     */
    data class Reading(
        val type: Type,
        val timeframe: Timeframe?,
        val value: BigDecimal?,
        val signal: Signal,
        val subLabel: String?,
        val updatedAt: DateTime?,
    ) {

        /**
         * Technical (RSI, MACD, MA_CROSS) and social (GALAXY_SCORE, SENTIMENT) indicator types
         */
        enum class Type {
            RSI,
            MACD,
            MA_CROSS,
            GALAXY_SCORE,
            SENTIMENT,
        }

        /**
         * Timeframe of an indicator reading: `24h` / `7d` / `1m` on the wire
         */
        enum class Timeframe {
            DAY,
            WEEK,
            MONTH,
        }

        /**
         * Interpreted indicator signal
         */
        enum class Signal {

            BULLISH,

            BEARISH,

            NEUTRAL,

            /** Not enough history to compute the indicator (e.g. MA cross without SMA200) */
            INSUFFICIENT_DATA,

            /** Indicator is not meaningful for the asset (stablecoins) */
            NOT_APPLICABLE,

            /** No fresh data (2+ consecutive sync misses) */
            NOT_AVAILABLE,
        }
    }
}