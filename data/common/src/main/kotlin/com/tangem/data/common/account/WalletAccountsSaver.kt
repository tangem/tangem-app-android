package com.tangem.data.common.account

import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.SaveWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Saver for wallet accounts
 *
[REDACTED_AUTHOR]
 */
interface WalletAccountsSaver {

    /** Store wallet accounts [response] by [userWalletId] */
    suspend fun store(userWalletId: UserWalletId, response: GetWalletAccountsResponse)

    /** Push wallet accounts [body] by [userWalletId] */
    @Throws
    suspend fun push(userWalletId: UserWalletId, body: SaveWalletAccountsResponse): GetWalletAccountsResponse?

    /** Push wallet accounts [accounts] by [userWalletId] */
    @Throws
    suspend fun push(userWalletId: UserWalletId, accounts: List<WalletAccountDTO>): GetWalletAccountsResponse?
}