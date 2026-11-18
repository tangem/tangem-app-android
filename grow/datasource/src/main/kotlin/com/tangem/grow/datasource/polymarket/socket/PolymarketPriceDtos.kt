package com.tangem.grow.datasource.polymarket.socket

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One price stream of the live-data socket and the key of its subscription.
 *
 * @property channel `price.crypto` for spot prices, `price.crypto.twap` for time-weighted ones
 * @property symbol the coin's pair, e.g. `btcusd`
 * @property provider price provider the stream is pinned to, when one is named
 * @property windowSeconds averaging window of a TWAP stream
 */
data class PolymarketPriceFeedKey(
    val channel: String,
    val symbol: String,
    val provider: String? = null,
    val windowSeconds: Int? = null,
)

/** First frame of every connection: price channels are served to CLOB API credentials only. */
@Serializable
class PolymarketPriceAuthDto(
    @SerialName("auth") val credentials: Credentials,
) {

    @SerialName("op")
    val operation: String = "auth"

    @Serializable
    data class Credentials(
        @SerialName("apiKey") val apiKey: String,
        @SerialName("secret") val secret: String,
        @SerialName("passphrase") val passphrase: String,
    ) {

        override fun toString(): String = "Credentials(REDACTED)"
    }
}

@Serializable
data class PolymarketPriceSubscriptionsDto(
    @SerialName("op") val operation: Operation,
    @SerialName("subscriptions") val subscriptions: List<Subscription>,
) {

    @Serializable
    enum class Operation {
        @SerialName("subscribe")
        SUBSCRIBE,

        @SerialName("unsubscribe")
        UNSUBSCRIBE,
    }

    @Serializable
    data class Subscription(
        @SerialName("channel") val channel: String,
        @SerialName("filter") val filter: Filter,
    )

    @Serializable
    data class Filter(
        @SerialName("symbol") val symbol: String,
        @SerialName("provider") val provider: String? = null,
        @SerialName("window_seconds") val windowSeconds: Int? = null,
    )
}

/**
 * Every frame of the socket. The server has no single discriminator: an answer to an operation carries [op]
 * (`authed`, `subscribed`, `error` with a [code]), a price frame carries [channel] and [payload].
 */
@Serializable
data class PolymarketPriceMessageDto(
    @SerialName("op") val op: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("channel") val channel: String? = null,
    @SerialName("payload") val payload: Payload? = null,
) {

    /**
     * A tick carries one price in [timestamp] and [value]; the snapshot sent on subscription carries the last two
     * minutes in [points] instead. Timestamps are epoch milliseconds. [fullAccuracyValue] is the exact price and
     * wins over the rounded [value] when present.
     */
    @Serializable
    data class Payload(
        @SerialName("symbol") val symbol: String? = null,
        @SerialName("timestamp") val timestamp: Double? = null,
        @SerialName("value") val value: Double? = null,
        @SerialName("full_accuracy_value") val fullAccuracyValue: String? = null,
        @SerialName("source") val source: String? = null,
        @SerialName("window_seconds") val windowSeconds: Int? = null,
        @SerialName("data") val points: List<Point>? = null,
    )

    @Serializable
    data class Point(
        @SerialName("timestamp") val timestamp: Double,
        @SerialName("value") val value: Double? = null,
        @SerialName("full_accuracy_value") val fullAccuracyValue: String? = null,
    )

    companion object {
        const val OP_ERROR = "error"
    }
}