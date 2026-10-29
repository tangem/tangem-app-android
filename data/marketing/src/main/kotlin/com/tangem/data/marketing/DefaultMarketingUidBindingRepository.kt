package com.tangem.data.marketing

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.marketing.models.BindMarketingUidWalletsBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class DefaultMarketingUidBindingRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val featureToggles: MarketingFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) : MarketingUidBindingRepository {

    private val pendingUids = MutableStateFlow(emptySet<String>())

    override suspend fun bind(uid: String) {
        if (!featureToggles.isUidBindingEnabled) return

        pendingUids.update { it + uid }
        sendPending()
    }

    override suspend fun bindPending() {
        if (!featureToggles.isUidBindingEnabled) return

        sendPending()
    }

    private suspend fun sendPending() = withContext(dispatchers.io) {
        runSuspendCatching { sendPendingToWallets() }
            .onFailure { TangemLogger.e("Unable to send marketing uid bindings", it) }
    }

    private suspend fun sendPendingToWallets() {
        if (pendingUids.value.isEmpty()) return

        val walletIds = userWalletsListRepository.userWalletsSync().map { it.walletId.stringValue }
        if (walletIds.isEmpty()) return

        pendingUids.value.forEach { uid ->
            // claimed one by one: a concurrent call never sends it twice, an interrupted one keeps the rest pending
            if (uid !in pendingUids.getAndUpdate { it - uid }) return@forEach

            val response = tangemTechApi.bindMarketingUidWallets(uid, BindMarketingUidWalletsBody(walletIds))
            if (response is ApiResponse.Error) TangemLogger.e("Marketing uid binding failed", response.cause)
        }
    }
}