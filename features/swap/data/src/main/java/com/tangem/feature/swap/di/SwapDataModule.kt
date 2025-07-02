package com.tangem.feature.swap.di

import com.squareup.moshi.Moshi
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.feature.swap.DefaultSwapRepository
import com.tangem.feature.swap.DefaultSwapTransactionRepository
import com.tangem.feature.swap.converters.ErrorsDataConverter
import com.tangem.feature.swap.domain.SwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class SwapDataModule {

    @Provides
    @Singleton
    internal fun provideSwapRepository(
        tangemExpressApi: TangemExpressApi,
        coroutineDispatcher: CoroutineDispatcherProvider,
        dataSignature: DataSignatureVerifier,
        walletManagerFacade: WalletManagersFacade,
        userWalletsListManager: UserWalletsListManager,
        errorsDataConverter: ErrorsDataConverter,
        @NetworkMoshi moshi: Moshi,
        excludedBlockchains: ExcludedBlockchains,
        appPreferencesStore: AppPreferencesStore,
    ): SwapRepository {
        return DefaultSwapRepository(
            tangemExpressApi = tangemExpressApi,
            coroutineDispatcher = coroutineDispatcher,
            walletManagersFacade = walletManagerFacade,
            userWalletsListManager = userWalletsListManager,
            errorsDataConverter = errorsDataConverter,
            dataSignatureVerifier = dataSignature,
            moshi = moshi,
            excludedBlockchains = excludedBlockchains,
            appPreferencesStore = appPreferencesStore,
        )
    }

    @Provides
    @Singleton
    fun provideSwapTransactionRepository(
        appPreferencesStore: AppPreferencesStore,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
        dispatcherProvider: CoroutineDispatcherProvider,
    ): SwapTransactionRepository {
        return DefaultSwapTransactionRepository(
            appPreferencesStore = appPreferencesStore,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
            dispatchers = dispatcherProvider,
        )
    }

    @Provides
    @Singleton
    internal fun provideErrorsConverter(@NetworkMoshi moshi: Moshi): ErrorsDataConverter {
        val jsonAdapter = moshi.adapter(ExpressErrorResponse::class.java)
        return ErrorsDataConverter(jsonAdapter)
    }
}
