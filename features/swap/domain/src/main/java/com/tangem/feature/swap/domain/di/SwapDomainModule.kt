package com.tangem.feature.swap.domain.di

import com.tangem.feature.swap.domain.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class SwapDomainModule {

    @Provides
    @Singleton
    fun provideAllowPermissionsHandler(): AllowPermissionsHandler {
        return AllowPermissionsHandlerImpl()
    }

    @Provides
    @Singleton
    fun provideSwapInteractorFactory(factory: SwapInteractorImpl.Factory): SwapInteractor.Factory {
        return factory
    }

    @Provides
    @Singleton
    fun provideInitialToCurrencyResolver(
        swapTransactionRepository: SwapTransactionRepository,
    ): InitialToCurrencyResolver {
        return DefaultInitialToCurrencyResolver(
            swapTransactionRepository = swapTransactionRepository,
        )
    }
}