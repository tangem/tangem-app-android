package com.tangem.domain.polymarket.model

/**
 * Typed error surface of the withdrawal endpoints. Unlike the onboarding batch, every `422` recovers the
 * same way — re-fetch the nonce, re-sign, resubmit — so the relayer's rejection is one variant carrying
 * the ProblemDetail message rather than a family of them.
 */
sealed interface PredictionWithdrawError : PolymarketError {

    data class InvalidRequest(val detail: String?) : PredictionWithdrawError

    data object Unauthorized : PredictionWithdrawError

    /** `409` — the (deposit wallet, nonce) pair is taken by a different withdrawal. Never re-sign at it. */
    data class NonceTaken(val detail: String?) : PredictionWithdrawError

    /** `422` — nothing was accepted and nothing recorded: re-fetch the nonce, re-sign, resubmit. */
    data class RelayerRejected(val detail: String?) : PredictionWithdrawError

    data object RelayerUnavailable : PredictionWithdrawError

    /** `404` — the id is unknown **or belongs to another wallet**; never "not yet created". */
    data object NotFound : PredictionWithdrawError

    data object Network : PredictionWithdrawError

    data class Unexpected(val httpCode: Int?, val detail: String?) : PredictionWithdrawError
}