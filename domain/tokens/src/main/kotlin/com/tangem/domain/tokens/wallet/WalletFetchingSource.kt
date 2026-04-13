package com.tangem.domain.tokens.wallet

import com.tangem.domain.tokens.FetchingSource

/**
 * Sealed class representing different types of fetching sources for wallet balance operations.
 *
 * This separates TangemPay (which requires special handling) from standard balance fetching sources
 * (NETWORK, QUOTE, STAKING) that are processed uniformly via [BalanceFetchingOperations].
 *
[REDACTED_AUTHOR]
 */
sealed class WalletFetchingSource {

    /**
     * TangemPay account fetching source.
     * Handled separately from standard balance sources via [com.tangem.domain.pay.flow.PaymentAccountStatusFetcher].
     */
    data object TangemPay : WalletFetchingSource()

    /**
     * Standard balance fetching sources (NETWORK, QUOTE, STAKING).
     * Processed via [BalanceFetchingOperations.fetchAll].
     *
     * @property sources set of [FetchingSource] types to fetch
     */
    data class Balance(val sources: Set<FetchingSource>) : WalletFetchingSource()
}