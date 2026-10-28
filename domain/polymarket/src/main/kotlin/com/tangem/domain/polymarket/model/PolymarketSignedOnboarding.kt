package com.tangem.domain.polymarket.model

import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload

/**
 * The outcome of the onboarding signing session; every field is reused verbatim by the steps that follow.
 */
data class PolymarketSignedOnboarding(
    val clobAuth: SignedClobAuth?,
    val approvals: SignedApprovals?,
) {

    data class SignedClobAuth(val l1Signature: String, val timestamp: String)

    data class SignedApprovals(val payload: PolymarketApprovalsPayload, val signature: String)
}