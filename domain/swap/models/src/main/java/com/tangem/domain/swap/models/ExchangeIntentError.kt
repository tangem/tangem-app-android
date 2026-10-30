package com.tangem.domain.swap.models

/** Why an [ExchangeIntent] could not be read. */
sealed class ExchangeIntentError(override val message: String) : Throwable(message) {

    /** Express does not know the intent. */
    data class NotFound(val intentId: String) : ExchangeIntentError("Intent $intentId not found")

    /** Express failed on its side (HTTP 5xx), so reading the intent again may help. */
    data class ServerError(val intentId: String, val httpCode: Int) :
        ExchangeIntentError("Express failed with HTTP $httpCode reading intent $intentId")

    /** Express sends the amount of one side only; the response carried both or neither. */
    data class AmbiguousAmountSide(val intentId: String) :
        ExchangeIntentError("Intent $intentId must carry the amount of exactly one side")
}