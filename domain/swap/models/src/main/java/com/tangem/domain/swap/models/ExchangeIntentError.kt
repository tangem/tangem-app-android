package com.tangem.domain.swap.models

/** Why an [ExchangeIntent] could not be read. */
sealed class ExchangeIntentError(override val message: String) : Throwable(message) {

    /** Express does not know the intent; [code] is the Express error code that said so. */
    data class NotFound(val intentId: String, val code: Int) : ExchangeIntentError("Intent $intentId not found")

    /** The intent was made for another wallet of the user; [code] is the Express error code that said so. */
    data class OtherWallet(val intentId: String, val code: Int) :
        ExchangeIntentError("Intent $intentId belongs to another wallet")

    /** There is no connection or Express did not answer in time, so reading the intent again may help. */
    data class Unreachable(val intentId: String) :
        ExchangeIntentError("Express is unreachable reading intent $intentId")

    /** Express sends the amount of one side only; the response carried both or neither. */
    data class AmbiguousAmountSide(val intentId: String) :
        ExchangeIntentError("Intent $intentId must carry the amount of exactly one side")
}