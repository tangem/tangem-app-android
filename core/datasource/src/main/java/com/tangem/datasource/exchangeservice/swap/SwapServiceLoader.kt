package com.tangem.datasource.exchangeservice.swap

import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.StateFlow

/**
 * Swap service loader
 *
[REDACTED_AUTHOR]
 */
interface SwapServiceLoader {

    /** Update service using [userWalletId] and [userTokens] */
    suspend fun update(userWalletId: UserWalletId, userTokens: UserTokensResponse)

    /** Get initialization status by [userWalletId] */
    fun getInitializationStatus(userWalletId: UserWalletId): StateFlow<Lce<Throwable, List<Asset>>>
}