package com.tangem.feature.swap.di

import com.squareup.moshi.Moshi
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
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
        )
    }

    @Provides
    @Singleton
    fun provideSwapTransactionRepository(
        appPreferencesStore: AppPreferencesStore,
        dispatcherProvider: CoroutineDispatcherProvider,
        excludedBlockchains: ExcludedBlockchains,
    ): SwapTransactionRepository {
        return DefaultSwapTransactionRepository(
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatcherProvider,
            excludedBlockchains = excludedBlockchains,
        )
    }

    @Provides
    @Singleton
    internal fun provideErrorsConverter(@NetworkMoshi moshi: Moshi): ErrorsDataConverter {
        val jsonAdapter = moshi.adapter(ExpressErrorResponse::class.java)
        return ErrorsDataConverter(jsonAdapter)
    }
}