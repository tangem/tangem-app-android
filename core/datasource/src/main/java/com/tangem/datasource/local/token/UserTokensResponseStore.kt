package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Store of [UserTokensResponse]
 *
[REDACTED_AUTHOR]
 */
interface UserTokensResponseStore {

    fun get(userWalletId: UserWalletId): Flow<UserTokensResponse?>

    /** Get [UserTokensResponse] synchronously by [userWalletId] or null */
    suspend fun getSyncOrNull(userWalletId: UserWalletId): UserTokensResponse?

    suspend fun store(userWalletId: UserWalletId, response: UserTokensResponse)

    suspend fun clear(userWalletId: UserWalletId)
}