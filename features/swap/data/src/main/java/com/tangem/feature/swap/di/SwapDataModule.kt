package com.tangem.feature.swap.di

import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.api.oneinch.OneInchErrorsHandler
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.config.ConfigManager
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.feature.swap.SwapRepositoryImpl
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SwapDataModule {

    @Provides
    @Singleton
    fun provideSwapRepository(
        tangemTechApi: TangemTechApi,
        tangemExpressApi: TangemExpressApi,
        oneInchApiFactory: OneInchApiFactory,
        oneInchErrorsHandler: OneInchErrorsHandler,
        coroutineDispatcher: CoroutineDispatcherProvider,
        configManager: ConfigManager,
        walletManagerFacade: WalletManagersFacade,
        walletsStateHolder: WalletsStateHolder
    ): SwapRepository {
        return SwapRepositoryImpl(
            tangemTechApi = tangemTechApi,
            tangemExpressApi = tangemExpressApi,
            oneInchApiFactory = oneInchApiFactory,
            oneInchErrorsHandler = oneInchErrorsHandler,
            coroutineDispatcher = coroutineDispatcher,
            configManager = configManager,
            walletManagersFacade = walletManagerFacade,
            walletsStateHolder = walletsStateHolder,
        )
    }
}