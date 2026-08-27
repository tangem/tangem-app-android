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
     * @param name      human-readable indicator name for display, as sent by the backend
     * @param timeframe reading timeframe — present for every indicator type
     * @param value     RSI value; MACD histogram; galaxy/sentiment score (0–100);
     *                  MA_CROSS deviation of SMA50 from SMA200, in percent.
     *                  `null` for non-signal states
     * @param signal    interpreted signal
     * @param updatedAt timestamp of the last stored value, or `null`
     */
    data class Reading(
        val type: Type,
        val name: String,
        val timeframe: Timeframe,
        val value: BigDecimal?,
        val signal: Signal,
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

            POSITIVE,

            NEGATIVE,

            NEUTRAL,

            /** Not enough history to compute the indicator (e.g. MA cross without SMA200) */
            INSUFFICIENT_DATA,

            /** No data to interpret: not meaningful for the asset (stablecoins), or no fresh data */
            NOT_AVAILABLE,
        }
    }
}