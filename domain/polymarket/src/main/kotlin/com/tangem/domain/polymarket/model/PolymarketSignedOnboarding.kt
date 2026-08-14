package com.tangem.domain.polymarket.model

import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload

/**
 * The outcome of the onboarding signing session. Besides the signatures it carries the two values that
 * must be reused verbatim: [clobAuthTimestamp] goes into the credentials request, and [approvals] is the
 * exact payload the signature covers — the submitted batch has to match it field for field.
 */
data class PolymarketSignedOnboarding(
    val l1Signature: String,
    val clobAuthTimestamp: String,
    val batchSignature: String,
    val approvals: PolymarketApprovalsPayload,
)