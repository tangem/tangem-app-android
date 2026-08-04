package com.tangem.domain.polymarket.model

/**
 * Typed error surface of the Polymarket BFF wallet endpoints (`/wallet`, `/wallet/deploy`,
 * `/wallet/approvals`). The BFF reports failures as RFC-7807 ProblemDetail with a real HTTP status;
 * these variants let callers (the onboarding orchestrator) react with an exhaustive `when` instead of
 * inspecting raw codes and parsing message strings.
 */
sealed interface PolymarketWalletError : PolymarketError {

    /** `400` — malformed request (bad params). Not user-recoverable; a client bug. */
    data object InvalidRequest : PolymarketWalletError

    /** `401` — missing/invalid api-key. */
    data object Unauthorized : PolymarketWalletError

    /**
     * `409` (Conflict) — the deposit wallet is not deployed yet, so the requested operation cannot proceed.
     * The canonical case is `approvals` before the DW is deployed; this is the generic mapping for any `409`.
     */
    data object WalletNotDeployed : PolymarketWalletError

    /**
     * `422` — the relayer rejected the signed batch. Client-fixable; the specific variant tells how to
     * recover (see the BFF retry table).
     */
    sealed interface RelayerRejected : PolymarketWalletError {

        /** Deploy not mined yet — wait until the wallet is `DEPLOYED`, then resubmit. */
        data object WalletNotRegistered : RelayerRejected

        /** Deadline too near/past — re-fetch the nonce, rebuild with a later deadline, re-sign, resubmit. */
        data object DeadlineTooSoon : RelayerRejected

        /** Signed `{depositWalletAddress, nonce, deadline, calls}` ≠ sent — re-check field-for-field, re-sign. */
        data object InvalidSignature : RelayerRejected

        /** Nonce already consumed — re-fetch the nonce, rebuild, re-sign. */
        data object NonceReused : RelayerRejected

        /** Any other `422` rejection (defensive fallback; carries the raw ProblemDetail `detail`). */
        data class Other(val detail: String?) : RelayerRejected
    }

    /** `502` — relayer/RPC unreachable or circuit breaker open. Retry later. */
    data object RelayerUnavailable : PolymarketWalletError

    /** No connectivity / timeout reaching the BFF. */
    data object Network : PolymarketWalletError

    /** Any other/unexpected failure (unmapped status or non-API error). */
    data class Unexpected(val httpCode: Int?, val detail: String?) : PolymarketWalletError
}