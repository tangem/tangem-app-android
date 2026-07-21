package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class GetCoinIndicatorsResponse(
    @Json(name = "assets") val assets: List<Asset>,
) {

    @JsonClass(generateAdapter = true)
    data class Asset(
        @Json(name = "symbol") val symbol: String,
        @Json(name = "indicators") val indicators: List<Indicator>,
    ) {

        /**
         * Single indicator reading for an asset
         *
         * @param type      indicator type
         * @param timeframe `24h`/`7d`/`1m` for RSI and MACD; `null` for the timeframe-agnostic
         *                  indicators (MA_CROSS, GALAXY_SCORE, SENTIMENT)
         * @param value     RSI value; MACD histogram; galaxy/sentiment score (0–100).
         *                  Always `null` for MA_CROSS and for `not_applicable`/`na`/`insufficient_data`
         * @param label     interpreted signal
         * @param subLabel  RSI only: `Overbought` / `Oversold`. `null` otherwise
         * @param updatedAt ISO-8601 timestamp of the last stored value, or `null`
         */
        @JsonClass(generateAdapter = true)
        data class Indicator(
            @Json(name = "type") val type: Type,
            @Json(name = "timeframe") val timeframe: Timeframe?,
            @Json(name = "value") val value: BigDecimal?,
            @Json(name = "label") val label: Signal,
            @Json(name = "subLabel") val subLabel: String?,
            @Json(name = "updatedAt") val updatedAt: DateTime?,
        ) {

            @JsonClass(generateAdapter = false)
            enum class Type {
                @Json(name = "rsi") RSI,

                @Json(name = "macd") MACD,

                @Json(name = "ma_cross") MA_CROSS,

                @Json(name = "galaxy_score") GALAXY_SCORE,

                @Json(name = "sentiment") SENTIMENT,

                UNKNOWN,
            }

            @JsonClass(generateAdapter = false)
            enum class Timeframe {
                @Json(name = "24h") H24,

                @Json(name = "7d") D7,

                @Json(name = "1m") M1,

                UNKNOWN,
            }

            /**
             * Interpreted indicator signal
             *
             * `bullish`/`bearish`/`neutral` — interpreted signal;
             * `insufficient_data` — not enough history (e.g. MA cross without SMA200);
             * `not_applicable` — indicator not meaningful for the asset (stablecoins);
             * `na` — no fresh data (2+ consecutive sync misses)
             */
            @JsonClass(generateAdapter = false)
            enum class Signal {
                @Json(name = "bullish") BULLISH,

                @Json(name = "bearish") BEARISH,

                @Json(name = "neutral") NEUTRAL,

                @Json(name = "insufficient_data") INSUFFICIENT_DATA,

                @Json(name = "not_applicable") NOT_APPLICABLE,

                @Json(name = "na") NOT_AVAILABLE,

                UNKNOWN,
            }
        }
    }
}