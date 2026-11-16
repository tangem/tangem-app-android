package com.tangem.domain.tokens

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Formats fetch errors into a human-readable log message.
 *
[REDACTED_AUTHOR]
 */
object FetchErrorFormatter {

    /**
     * Formats fetch errors into a log message.
     *
     * @param userWalletId the user wallet identifier for context
     * @param errors the map of source name to error (keys: [FetchingSource] names for network and quote,
     * the [com.tangem.domain.models.currency.balance.BalanceContributionFetcher] class simple name for contributions)
     * @return formatted error message
     */
    fun format(userWalletId: UserWalletId, errors: Map<String, Throwable>): String {
        return formatInternal(userWalletId = userWalletId, entries = errors.toList())
    }

    private fun formatInternal(userWalletId: UserWalletId, entries: List<Pair<String, Throwable>>): String {
        return "Failed to fetch next sources for $userWalletId:\n" +
            entries.joinToString(separator = "\n") { (name, error) -> "$name – $error" }
    }
}