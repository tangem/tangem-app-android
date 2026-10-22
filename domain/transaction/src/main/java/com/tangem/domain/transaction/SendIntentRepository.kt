package com.tangem.domain.transaction

import arrow.core.Either
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.domain.transaction.models.SendIntent

/** Transfers AI agents have asked the wallets of the user to make. */
interface SendIntentRepository {

    /**
     * Reads the send intent [intentId].
     *
     * The service picks the wallets it answers for from the mobile session, so no wallet is passed. An unknown
     * intent and a malformed amount are a [SendIntentError]; anything else, including a response that cannot be
     * parsed, is the raw failure of the call.
     */
    suspend fun getSendIntent(intentId: String): Either<Throwable, SendIntent>

    /**
     * Links [txHash], the transaction sent for the intent [intentId], to it, which completes the intent.
     *
     * Reporting the same hash again succeeds. An unknown intent and an intent that already carries another
     * transaction are a [SendIntentError]; anything else is the raw failure of the call.
     */
    suspend fun completeSendIntent(intentId: String, txHash: String): Either<Throwable, Unit>

    /**
     * Like [completeSendIntent], but tries again, with a pause, while the failure may pass: no network, a timeout,
     * HTTP 5xx, 401, 408 or 429. Anything else, and the last failure once the attempts are used up, is returned as it
     * is. The attempts live as long as the caller, so they do not survive the process.
     */
    suspend fun reportSendIntentHash(intentId: String, txHash: String): Either<Throwable, Unit>
}