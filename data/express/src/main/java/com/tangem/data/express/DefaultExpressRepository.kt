package com.tangem.data.express

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.express.converter.ExpressProviderConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.exchangeservice.swap.ExpressUtils
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.filterIf
import timber.log.Timber

internal class DefaultExpressRepository(
    private val tangemExpressApi: TangemExpressApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : ExpressRepository {

    override suspend fun getProviders(
        userWallet: UserWallet,
        filterProviderTypes: List<ExpressProviderType>,
    ): List<ExpressProvider> = with(dispatchers.io) {
        safeApiCall(
            call = {
                tangemExpressApi.getProviders(
                    userWalletId = userWallet.walletId.stringValue,
                    refCode = ExpressUtils.getRefCode(
                        userWallet = userWallet,
                        appPreferencesStore = appPreferencesStore,
                    ),
                ).getOrThrow().map(ExpressProviderConverter()::convert)
                    .filterIf(filterProviderTypes.isNotEmpty()) { it.type in filterProviderTypes }
            },
            onError = {
                Timber.w(it, "Unable to fetch express providers")
                throw it
            },
        )
    }
}