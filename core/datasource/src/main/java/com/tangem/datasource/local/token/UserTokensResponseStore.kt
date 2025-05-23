package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Store of [UserTokensResponse]
 *
[REDACTED_AUTHOR]
 */
interface UserTokensResponseStore {

    /** Get [UserTokensResponse] synchronously by [userWalletId] or null */
    suspend fun getSyncOrNull(userWalletId: UserWalletId): UserTokensResponse?
}