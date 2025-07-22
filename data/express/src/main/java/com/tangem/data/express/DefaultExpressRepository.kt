package com.tangem.data.express

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.express.converter.ExpressProviderConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.exchangeservice.swap.ExpressUtils
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.wallets.models.UserWallet
import timber.log.Timber

internal class DefaultExpressRepository(
    private val tangemExpressApi: TangemExpressApi,
    private val appPreferencesStore: AppPreferencesStore,
) : ExpressRepository {

    override suspend fun getProviders(userWallet: UserWallet): List<ExpressProvider> {
        return safeApiCall(
            call = {
                tangemExpressApi.getProviders(
                    userWalletId = userWallet.walletId.stringValue,
                    refCode = ExpressUtils.getRefCode(
                        userWallet = userWallet,
                        appPreferencesStore = appPreferencesStore,
                    ),
                ).getOrThrow().map(ExpressProviderConverter()::convert)
            },
            onError = {
                Timber.w(it, "Unable to fetch express providers")
                throw it
            },
        )
    }
}