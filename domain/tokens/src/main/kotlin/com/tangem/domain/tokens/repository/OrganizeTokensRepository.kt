package com.tangem.domain.tokens.repository

import com.tangem.domain.models.userwallet.UserWalletId
import com.tangem.domain.tokens.model.Token

interface OrganizeTokensRepository {

    @Suppress("unused") // TODO
    suspend fun sortTokens(
        userWalletId: UserWalletId,
        tokens: Set<Token.ID>,
        isGrouped: Boolean,
        isSortedByBalance: Boolean,
    )

    @Suppress("unused") // TODO
    suspend fun isGrouped(userWalletId: UserWalletId): Boolean

    @Suppress("unused") // TODO
    suspend fun isSortedByBalance(userWalletId: UserWalletId): Boolean
}
