package com.tangem.data.marketing

import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.datasource.api.marketing.models.BindMarketingUidWalletsBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

internal class DefaultMarketingUidBindingRepository(
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val featureToggles: MarketingFeatureToggles,
    private val walletServerBinder: WalletServerBinder,
) : MarketingUidBindingRepository {

    override suspend fun bind(uid: String) {
        if (!featureToggles.isUidBindingEnabled) return

        val wallets = userWalletsListRepository.userWallets.filterNotNull().first().ifEmpty { awaitFirstWallets() }
        val body = BindMarketingUidWalletsBody(walletIds = wallets.map { it.walletId.stringValue })

        val response = tangemTechApi.bindMarketingUidWallets(uid, body)
        if (response is ApiResponse.Error) TangemLogger.e("Marketing uid binding failed", response.cause)
    }

    // A new wallet is created on the server in the background, and the server drops a binding with a wallet it lacks
    private suspend fun awaitFirstWallets(): List<UserWallet> {
        val wallets = userWalletsListRepository.userWallets.filterNotNull().first { it.isNotEmpty() }
        wallets.forEach { walletServerBinder.awaitBound(it.walletId) }
        return wallets
    }
}