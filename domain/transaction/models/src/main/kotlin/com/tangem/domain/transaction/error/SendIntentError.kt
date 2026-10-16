package com.tangem.domain.transaction.error

/** Why a [com.tangem.domain.transaction.models.SendIntent] could not be read. */
sealed class SendIntentError(override val message: String) : Throwable(message) {

    /** The service does not tell an unknown intent from an intent raised for another wallet. */
    data class NotFound(val intentId: String) : SendIntentError("Intent $intentId not found or of another wallet")

    /** The owner revoked the session of the agent that raised the intent. */
    data class SessionRevoked(val intentId: String) :
        SendIntentError("Session of the agent behind intent $intentId is revoked")

    /** The amount is not a positive decimal number. */
    data class MalformedAmount(val intentId: String, val amount: String) :
        SendIntentError("Intent $intentId carries a malformed amount: $amount")
}