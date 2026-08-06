package com.tangem.domain.polymarket.signing

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketOnboardingSignatures
import com.tangem.domain.polymarket.model.PolymarketSigningError

/**
 * Signs the Polymarket onboarding payloads with the owner EOA key. The key must already be derived;
 * signing never derives it.
 */
interface PolymarketTypedDataSigner {

    /** Signs both onboarding payloads within a single signing session. */
    suspend fun signOnboarding(
        userWalletId: UserWalletId,
        clobAuth: PolymarketClobAuthData,
        approvals: PolymarketApprovalsPayload,
    ): Either<PolymarketSigningError, PolymarketOnboardingSignatures>

    /** Signs only the `ClobAuth` payload — used when re-issuing API credentials. */
    suspend fun signClobAuth(
        userWalletId: UserWalletId,
        clobAuth: PolymarketClobAuthData,
    ): Either<PolymarketSigningError, String>
}

/** Variable part of the `ClobAuth` payload; the signed address comes from the signing key. */
data class PolymarketClobAuthData(
    val timestamp: String,
    val nonce: String = "0",
)

/** Unsigned half of the approvals batch. */
data class PolymarketApprovalsPayload(
    val depositWalletAddress: String,
    val nonce: String,
    val deadline: String,
    val calls: List<PolymarketApprovalCall>,
)