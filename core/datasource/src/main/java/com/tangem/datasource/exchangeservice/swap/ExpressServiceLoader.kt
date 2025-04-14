package com.tangem.datasource.exchangeservice.swap

import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Express service loader
 *
[REDACTED_AUTHOR]
 */
interface ExpressServiceLoader {

    /** Update service using [userWalletId] and [userTokens] */
    suspend fun update(userWalletId: UserWalletId, userTokens: List<LeastTokenInfo>)

    /** Get initialization status by [userWalletId] */
    fun getInitializationStatus(userWalletId: UserWalletId): Flow<Lce<Throwable, List<Asset>>>
}