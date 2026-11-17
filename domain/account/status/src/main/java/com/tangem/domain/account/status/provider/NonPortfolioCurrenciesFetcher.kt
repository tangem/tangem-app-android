package com.tangem.domain.account.status.provider

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Fetches the currencies held outside the portfolio on [Network] — the fetching counterpart of
 * [NonPortfolioCurrenciesProvider], kept apart so read-only consumers never depend on a fetcher.
 *
 * A no-op for a network the holder does not use.
 */
interface NonPortfolioCurrenciesFetcher {

    suspend fun fetch(userWalletId: UserWalletId, network: Network)
}