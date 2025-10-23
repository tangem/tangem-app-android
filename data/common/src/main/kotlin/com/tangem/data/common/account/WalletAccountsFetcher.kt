package com.tangem.data.common.account

import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Component for fetching wallet accounts
 *
[REDACTED_AUTHOR]
 */
interface WalletAccountsFetcher {

    /** Fetch wallet accounts by [userWalletId] */
    @Throws
    suspend fun fetch(userWalletId: UserWalletId): GetWalletAccountsResponse

    /** Get saved wallet accounts by [userWalletId] */
    suspend fun getSaved(userWalletId: UserWalletId): GetWalletAccountsResponse?
}