package com.tangem.feature.swap.di

import com.squareup.moshi.Moshi
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.config.ConfigManager
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.feature.swap.SwapRepositoryImpl
import com.tangem.feature.swap.converters.ErrorsDataConverter
import com.tangem.feature.swap.DefaultSwapTransactionRepository
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.SwapTransactionRepository
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
        tangemTechApi: TangemTechApi,
        tangemExpressApi: TangemExpressApi,
        oneInchApiFactory: OneInchApiFactory,
        coroutineDispatcher: CoroutineDispatcherProvider,
        configManager: ConfigManager,
        walletManagerFacade: WalletManagersFacade,
        walletsStateHolder: WalletsStateHolder,
        errorsDataConverter: ErrorsDataConverter,
    ): SwapRepository {
        return SwapRepositoryImpl(
            tangemTechApi = tangemTechApi,
            tangemExpressApi = tangemExpressApi,
            oneInchApiFactory = oneInchApiFactory,
            coroutineDispatcher = coroutineDispatcher,
            configManager = configManager,
            walletManagersFacade = walletManagerFacade,
            walletsStateHolder = walletsStateHolder,
            errorsDataConverter = errorsDataConverter,
        )
    }

    @Provides
    @Singleton
    fun provideSwapTransactionRepository(appPreferencesStore: AppPreferencesStore): SwapTransactionRepository {
        return DefaultSwapTransactionRepository(
            appPreferencesStore = appPreferencesStore,
        )
    }

    @Provides
    @Singleton
    internal fun provideErrorsConverter(@NetworkMoshi moshi: Moshi): ErrorsDataConverter {
        val jsonAdapter = moshi.adapter(ExpressErrorResponse::class.java)
        return ErrorsDataConverter(jsonAdapter)
    }
}