package com.tangem.domain.tokens

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Formats fetch errors into a human-readable log message.
 *
[REDACTED_AUTHOR]
 */
object FetchErrorFormatter {

    /** Source name constant for TangemPay errors */
    const val TANGEM_PAY_SOURCE_NAME = "TANGEM_PAY"

    /**
     * Formats fetch errors into a log message.
     * Used by [CryptoCurrencyBalanceFetcher] which works only with [FetchingSource].
     *
     * @param userWalletId the user wallet identifier for context
     * @param errors the map of source to error
     * @return formatted error message
     */
    fun format(userWalletId: UserWalletId, errors: Map<FetchingSource, Throwable>): String {
        return formatInternal(
            userWalletId = userWalletId,
            entries = errors.map { (source, error) -> source.name to error },
        )
    }

    /**
     * Formats fetch errors into a log message.
     * Used by [WalletBalanceFetcher] which works with [WalletFetchingSource] including TangemPay.
     *
     * @param userWalletId the user wallet identifier for context
     * @param errors the map of source name to error (keys: "NETWORK", "QUOTE", "STAKING", "TANGEM_PAY")
     * @return formatted error message
     */
    fun formatWalletErrors(userWalletId: UserWalletId, errors: Map<String, Throwable>): String {
        return formatInternal(
            userWalletId = userWalletId,
            entries = errors.map { (sourceName, error) -> sourceName to error },
        )
    }

    private fun formatInternal(userWalletId: UserWalletId, entries: List<Pair<String, Throwable>>): String {
        return "Failed to fetch next sources for $userWalletId:\n" +
            entries.joinToString(separator = "\n") { (name, error) -> "$name – $error" }
    }
}