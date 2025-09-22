package com.tangem.domain.account.fetcher

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Component that fetches a list of accounts for multiple wallets by a set of [UserWalletId]s or
 * all accounts if no set is provided
 *
[REDACTED_AUTHOR]
 */
interface MultiAccountListFetcher : FlowFetcher<MultiAccountListFetcher.Params> {

    sealed interface Params {

        data class Set(val ids: kotlin.collections.Set<UserWalletId>) : Params

        data object All : Params
    }
}