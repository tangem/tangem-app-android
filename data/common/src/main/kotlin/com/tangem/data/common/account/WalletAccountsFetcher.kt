package com.tangem.data.common.account

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Component for fetching wallet accounts
 *
[REDACTED_AUTHOR]
 */
interface WalletAccountsFetcher {

    /** Fetch wallet accounts by [userWalletId] */
    @Throws
    suspend fun fetch(userWalletId: UserWalletId)
}