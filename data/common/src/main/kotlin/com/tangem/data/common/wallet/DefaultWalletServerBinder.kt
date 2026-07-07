package com.tangem.data.common.wallet

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ETAG_HEADER
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.converters.WalletIdBodyConverter
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

internal class DefaultWalletServerBinder(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val appsFlyerStore: AppsFlyerStore,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletServerBinder {

    override suspend fun bind(userWalletId: UserWalletId): ApiResponse<Unit>? {
        val userWallet = userWalletsListRepository.getSyncOrNull(id = userWalletId)

        if (userWallet == null) {
            TangemLogger.e("bind wallet=$userWalletId: user wallet not found locally, skipping createWallet call")
            return null
        }

        return bind(userWallet)
    }

    override suspend fun bind(userWallet: UserWallet): ApiResponse<Unit> {
        val conversionData = appsFlyerStore.get()

        return withContext(dispatchers.io) {
            tangemTechApi.createWallet(
                body = WalletIdBodyConverter.convert(userWallet, conversionData),
            )
        }.also { response ->
            val eTag = response.headers[ETAG_HEADER]?.firstOrNull()
            TangemLogger.i(
                "bind wallet=${userWallet.walletId}: createWallet code=${(response as? ApiResponse.Success)?.code}, " +
                    "hasETag=${eTag != null}, eTagNotEmpty=${!eTag.isNullOrEmpty()}",
            )
        }
    }
}