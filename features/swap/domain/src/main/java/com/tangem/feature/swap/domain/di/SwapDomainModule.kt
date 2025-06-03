package com.tangem.feature.swap.domain.di

import com.tangem.domain.tokens.GetCryptoCurrencyStatusesSyncUseCase
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
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
    fun providesGetCryptoCurrencyStatusUseCase(
        currencyStatusOperations: BaseCurrencyStatusOperations,
    ): GetCryptoCurrencyStatusesSyncUseCase {
        return GetCryptoCurrencyStatusesSyncUseCase(currencyStatusOperations)
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