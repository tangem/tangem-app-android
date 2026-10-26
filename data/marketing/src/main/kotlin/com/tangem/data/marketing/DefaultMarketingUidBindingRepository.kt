package com.tangem.data.marketing

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.marketing.models.BindMarketingUidWalletsBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

internal class DefaultMarketingUidBindingRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val featureToggles: MarketingFeatureToggles,
) : MarketingUidBindingRepository {

    override suspend fun bind(uid: String) {
        if (!featureToggles.isUidBindingEnabled) return

        val wallets = userWalletsListRepository.userWallets.filterNotNull().first { it.isNotEmpty() }
        val body = BindMarketingUidWalletsBody(walletIds = wallets.map { it.walletId.stringValue })

        val response = tangemTechApi.bindMarketingUidWallets(uid, body)
        if (response is ApiResponse.Error) TangemLogger.e("Marketing uid binding failed", response.cause)
    }
}