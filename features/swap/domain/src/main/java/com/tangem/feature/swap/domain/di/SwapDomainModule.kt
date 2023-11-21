package com.tangem.feature.swap.domain.di

import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.swap.domain.*
import com.tangem.feature.swap.domain.cache.SwapDataCacheImpl
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SwapDomainModule {

    @Provides
    @Singleton
    fun provideSwapInteractor(
        swapRepository: SwapRepository,
        userWalletManager: UserWalletManager,
        transactionManager: TransactionManager,
        currenciesRepository: CurrenciesRepository,
        networksRepository: NetworksRepository,
        @SwapScope getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    ): SwapInteractor {
        return SwapInteractorImpl(
            transactionManager = transactionManager,
            userWalletManager = userWalletManager,
            repository = swapRepository,
            cache = SwapDataCacheImpl(),
            allowPermissionsHandler = AllowPermissionsHandlerImpl(),
            currenciesRepository = currenciesRepository,
            networksRepository = networksRepository,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideBlockchainInteractor(transactionManager: TransactionManager): BlockchainInteractor {
        return BlockchainInteractorImpl(
            transactionManager = transactionManager,
        )
    }

    @SwapScope
    @Provides
    @Singleton
    fun providesGetSelectedWalletUseCase(walletsStateHolder: WalletsStateHolder): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(walletsStateHolder = walletsStateHolder)
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SwapScope