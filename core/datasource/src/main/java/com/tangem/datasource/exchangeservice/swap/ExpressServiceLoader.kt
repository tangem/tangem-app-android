package com.tangem.datasource.exchangeservice.swap

import com.tangem.datasource.api.express.models.request.LeastTokenInfo
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.StateFlow

/**
 * Express service loader
 *
 * @author Andrew Khokhlov on 06/11/2024
 */
interface ExpressServiceLoader {

    /** Update service using [userWalletId] and [userTokens] */
    suspend fun update(userWalletId: UserWalletId, userTokens: List<LeastTokenInfo>)

    /** Get initialization status by [userWalletId] */
    fun getInitializationStatus(userWalletId: UserWalletId): StateFlow<Lce<Throwable, List<Asset>>>
}
