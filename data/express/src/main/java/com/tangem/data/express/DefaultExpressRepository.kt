package com.tangem.data.express

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.express.converter.ExpressProviderConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.exchangeservice.swap.ExpressUtils
import com.tangem.datasource.local.converter.toEntity
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.filterIf
import com.tangem.utils.logging.TangemLogger

internal class DefaultExpressRepository(
    private val tangemExpressApi: TangemExpressApi,
    private val expressHistoryDao: ExpressHistoryDao,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles,
) : ExpressRepository {

    override suspend fun getProviders(
        userWallet: UserWallet,
        filterProviderTypes: List<ExpressProviderType>,
    ): List<ExpressProvider> = with(dispatchers.io) {
        safeApiCall(
            call = {
                val providers = tangemExpressApi.getProviders(
                    userWalletId = userWallet.walletId.stringValue,
                    refCode = ExpressUtils.getRefCode(
                        userWallet = userWallet,
                        appPreferencesStore = appPreferencesStore,
                    ),
                ).getOrThrow()

                if (txHistoryFeatureToggles.isNewTxHistoryEnabled) {
                    expressHistoryDao.upsertProviders(providers.map { it.toEntity() })
                }

                providers.map(ExpressProviderConverter()::convert)
                    .filterIf(filterProviderTypes.isNotEmpty()) { it.type in filterProviderTypes }
            },
            onError = { error ->
                TangemLogger.w("Unable to fetch express providers", error)
                throw error
            },
        )
    }
}