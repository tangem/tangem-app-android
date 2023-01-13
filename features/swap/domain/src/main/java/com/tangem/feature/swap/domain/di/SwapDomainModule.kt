package com.tangem.feature.swap.domain.di

import com.tangem.feature.swap.domain.AllowPermissionsHandlerImpl
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.SwapInteractorImpl
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.feature.swap.domain.cache.SwapDataCacheImpl
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.UserWalletManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    ): SwapInteractor {
        return SwapInteractorImpl(
            transactionManager = transactionManager,
            userWalletManager = userWalletManager,
            repository = swapRepository,
            cache = SwapDataCacheImpl(),
            allowPermissionsHandler = AllowPermissionsHandlerImpl(),
        )
    }
}
