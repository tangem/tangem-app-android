package com.tangem.data.common.wallet

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.converters.WalletIdBodyConverter
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultWalletServerBinder(
    private val userWalletsStore: UserWalletsStore,
    private val appsFlyerStore: AppsFlyerStore,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletServerBinder {

    override suspend fun bind(userWalletId: UserWalletId): ApiResponse<Unit>? {
        val userWallet = userWalletsStore.getSyncOrNull(key = userWalletId) ?: return null

        return bind(userWallet)
    }

    override suspend fun bind(userWallet: UserWallet): ApiResponse<Unit> {
        val conversionData = appsFlyerStore.get()

        return withContext(dispatchers.io) {
            tangemTechApi.createWallet(
                body = WalletIdBodyConverter.convert(userWallet, conversionData),
            )
        }
    }
}