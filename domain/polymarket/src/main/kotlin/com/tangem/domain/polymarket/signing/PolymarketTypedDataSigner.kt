package com.tangem.domain.polymarket.signing

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketOnboardingSignatures
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderDraft
import com.tangem.domain.polymarket.model.PredictionSignedOrder

interface PolymarketTypedDataSigner {

    /** Signs both onboarding payloads within a single signing session. */
    suspend fun signOnboarding(
        userWalletId: UserWalletId,
        clobAuth: PolymarketClobAuthData,
        approvals: PolymarketApprovalsPayload,
    ): Either<PolymarketSigningError, PolymarketOnboardingSignatures>

    suspend fun signOrder(
        userWalletId: UserWalletId,
        payload: PredictionOrderPayload,
    ): Either<PolymarketSigningError, PredictionSignedOrder>

    /** Signs only the `ClobAuth` payload — used when re-issuing API credentials. */
    suspend fun signClobAuth(
        userWalletId: UserWalletId,
        clobAuth: PolymarketClobAuthData,
    ): Either<PolymarketSigningError, String>

    /**
     * Signs a single `Batch` envelope — the same structure `POST /wallet/approvals` uses, with one call
     * instead of thirteen.
     *
     * @return the formatted `r+s+v` signature
     */
    suspend fun signRelayedBatch(
        userWalletId: UserWalletId,
        payload: PolymarketApprovalsPayload,
    ): Either<PolymarketSigningError, String>
}

data class PolymarketClobAuthData(
    val timestamp: String,
    val nonce: String = "0",
)

data class PredictionOrderPayload(
    val draft: PredictionOrderDraft,
    val depositWalletAddress: String,
    val salt: Long,
    val timestampMillis: Long,
)

data class PolymarketApprovalsPayload(
    val depositWalletAddress: String,
    val nonce: String,
    val deadline: String,
    val calls: List<PolymarketApprovalCall>,
)