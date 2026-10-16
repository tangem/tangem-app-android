package com.tangem.domain.swap.models

/** Why an [ExchangeIntent] could not be read. */
sealed class ExchangeIntentError(override val message: String) : Throwable(message) {

    /** Express sends the amount of one side only; the response carried both or neither. */
    data class AmbiguousAmountSide(val intentId: String) :
        ExchangeIntentError("Intent $intentId must carry the amount of exactly one side")
}