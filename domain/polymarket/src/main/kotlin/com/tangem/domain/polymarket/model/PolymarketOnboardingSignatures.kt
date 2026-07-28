package com.tangem.domain.polymarket.model

/** The two owner signatures produced during onboarding. */
data class PolymarketOnboardingSignatures(
    val l1Signature: String,
    val batchSignature: String,
)