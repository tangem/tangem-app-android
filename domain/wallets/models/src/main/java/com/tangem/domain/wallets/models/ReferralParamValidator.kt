package com.tangem.domain.wallets.models

/**
 * Validates referral params (`ref` / `refcode`, `campaign`) received from deep links and AppsFlyer.
 *
 * Link builders and attribution SDKs substitute a missing value with the literal `"null"`, so it is rejected the same
 * way as a blank value: storing it would bind the user's wallets to a junk referral code.
 */
object ReferralParamValidator {

    private const val NULL_LITERAL = "null"

    /** Returns the trimmed [value], or `null` if it is missing, blank or the literal `"null"` (case-insensitive) */
    fun sanitize(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() && !it.equals(NULL_LITERAL, ignoreCase = true) }
    }
}