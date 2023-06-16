package com.tangem.domain.tokens.repository

import com.tangem.domain.models.userwallet.UserWalletId
import com.tangem.domain.tokens.model.Token

interface TokensRepository {

    @Suppress("unused") // TODO
    suspend fun getTokens(userWalletId: UserWalletId, refresh: Boolean): Set<Token>
}
