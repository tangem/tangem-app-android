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
     * intent, a revoked agent session and a malformed amount are a [SendIntentError]; anything else, including a
     * response that cannot be parsed, is the raw failure of the call.
     */
    suspend fun getSendIntent(intentId: String): Either<Throwable, SendIntent>
}