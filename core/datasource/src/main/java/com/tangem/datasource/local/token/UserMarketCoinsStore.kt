package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.domain.wallets.models.UserWalletId

interface UserMarketCoinsStore {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): CoinsResponse?

    suspend fun store(userWalletId: UserWalletId, item: CoinsResponse)
}