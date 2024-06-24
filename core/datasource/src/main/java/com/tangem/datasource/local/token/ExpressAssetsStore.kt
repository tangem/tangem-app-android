package com.tangem.datasource.local.token

import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.domain.wallets.models.UserWalletId

interface ExpressAssetsStore {

    suspend fun getSyncOrNull(userWalletId: UserWalletId): List<Asset>?

    suspend fun store(userWalletId: UserWalletId, item: List<Asset>)
}