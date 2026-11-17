package com.tangem.domain.swap

/**
 * Providers allowed to swap with the collectibles vault, a program-owned address (PDA).
 * A CEX must pay out to it and refund to it: Changelly refuses an address without a private key,
 * and a refund to it can't be redirected later. DEX providers are not restricted.
 */
object SwapProviderCollectiblesPolicy {

    private val ALLOWED_CEX_PROVIDER_IDS = setOf("changehero")

    fun isCexAllowed(providerId: String): Boolean = providerId in ALLOWED_CEX_PROVIDER_IDS
}