package com.tangem.domain.polymarket.model

import arrow.core.Either

sealed interface PolymarketSessionKeyProgress {

    data object Signing : PolymarketSessionKeyProgress

    data class Revoking(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyProgress

    data class Revoked(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyProgress

    data class Authorizing(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyProgress

    data class CheckingReadiness(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyProgress

    /** @property result the key as the operation left it: `ACTIVE` and ready, or `REVOKED` after a revocation */
    data class Finished(
        val result: Either<PolymarketSessionKeyLifecycleError, PolymarketSessionKey>,
    ) : PolymarketSessionKeyProgress
}

sealed interface PolymarketSessionKeyLifecycleError : PolymarketError {

    data object KeyMissing : PolymarketSessionKeyLifecycleError

    data object OperationInProgress : PolymarketSessionKeyLifecycleError

    data class Failed(val cause: PolymarketSessionKeyError) : PolymarketSessionKeyLifecycleError

    /** @property cause the refusal of the BFF or the relayer, `null` when the signature expired before it was sent */
    data class StaleSignature(val cause: PolymarketSessionKeyError?) : PolymarketSessionKeyLifecycleError

    data class AuthorizationFailed(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyLifecycleError

    /** @property sessionKey the key back in its previous status, `null` if its record was dropped */
    data class RevocationFailed(val sessionKey: PolymarketSessionKey?) : PolymarketSessionKeyLifecycleError

    data class Unsettled(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyLifecycleError

    data class CredentialsUnavailable(
        val sessionKey: PolymarketSessionKey,
        val cause: PolymarketSessionCredentialsError,
    ) : PolymarketSessionKeyLifecycleError

    data class OwnerCredentialsUnavailable(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyLifecycleError

    data class NotListedOnExchange(val sessionKey: PolymarketSessionKey) : PolymarketSessionKeyLifecycleError
}