package com.tangem.domain.swap

/**
 * Changelly and ChangeHero screen addresses and reject the swap before signing. ChangeHero reports it with
 * [PROVIDER_REJECTED_ERROR_CODE]; Changelly has no code, so any error but user-fixable ones counts, `-1` included.
 */
object SwapProviderHidingPolicy {

    const val PROVIDER_REJECTED_ERROR_CODE = 2264

    private const val CHANGEHERO_PROVIDER_ID = "changehero"
    private const val CHANGELLY_PROVIDER_ID = "changelly"

    @Suppress("MagicNumber")
    private val CHANGELLY_USER_RESOLVABLE_ERROR_CODES = setOf(2250, 2251, 2252, 2270, 2280, 2310, 2320)

    fun shouldHide(providerId: String, errorCode: Int): Boolean = when (providerId) {
        CHANGEHERO_PROVIDER_ID -> errorCode == PROVIDER_REJECTED_ERROR_CODE
        CHANGELLY_PROVIDER_ID -> errorCode !in CHANGELLY_USER_RESOLVABLE_ERROR_CODES
        else -> false
    }
}