package com.tangem.domain.tokens

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Fetcher of crypto currencies for a multi-currency wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface MultiWalletCryptoCurrenciesFetcher : FlowFetcher<MultiWalletCryptoCurrenciesFetcher.Params> {

    data class Params(val userWalletId: UserWalletId)
}