package com.tangem.domain.swap

/** CEX providers allowed to swap with the collectibles vault, a program-owned address (PDA). */
object SwapProviderCollectiblesPolicy {

    /** A CEX must pay out to the PDA: Changelly refuses an address without a private key. */
    private val TOP_UP_CEX_PROVIDER_IDS = setOf("changehero")

    /** A CEX must credit a deposit the vault program sends, an inner transfer rather than a wallet's own. */
    private val WITHDRAWAL_CEX_PROVIDER_IDS = setOf("changelly")

    fun isTopUpCexAllowed(providerId: String): Boolean = providerId in TOP_UP_CEX_PROVIDER_IDS

    fun isWithdrawalCexAllowed(providerId: String): Boolean = providerId in WITHDRAWAL_CEX_PROVIDER_IDS
}