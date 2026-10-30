package com.tangem.domain.transaction.error

/** Why a [com.tangem.domain.transaction.models.SendIntent] could not be read or completed. */
sealed class SendIntentError(override val message: String) : Throwable(message) {

    /** The service does not tell an unknown intent from an intent raised for another wallet. */
    data class NotFound(val intentId: String) : SendIntentError("Intent $intentId not found or of another wallet")

    /** The intent already carries the hash of another transaction. */
    data class AlreadyCompleted(val intentId: String) :
        SendIntentError("Intent $intentId is already completed with another transaction")

    /** The service failed on its side (HTTP 5xx) reading the intent, so reading it again may help. */
    data class ServerError(val intentId: String, val httpCode: Int) :
        SendIntentError("Service failed with HTTP $httpCode reading intent $intentId")

    /** The amount is not a positive decimal number. */
    data class MalformedAmount(val intentId: String, val amount: String) :
        SendIntentError("Intent $intentId carries a malformed amount: $amount")
}