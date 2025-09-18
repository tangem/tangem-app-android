package com.tangem.domain.account.fetcher

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Component that fetches a list of accounts for a single wallet by [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleAccountListFetcher : FlowFetcher<SingleAccountListFetcher.Params> {

    data class Params(val userWalletId: UserWalletId)
}