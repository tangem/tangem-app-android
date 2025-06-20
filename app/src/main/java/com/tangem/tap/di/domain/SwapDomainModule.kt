package com.tangem.tap.di.domain

import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.usecase.GetSwapSupportedPairsUseCase
import com.tangem.feature.swap.domain.GetAvailablePairsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.tangem.feature.swap.domain.api.SwapRepository as OldSwapRepository

/**
[REDACTED_AUTHOR]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SwapDomainModule {

    @Provides
    @Singleton
    fun provideGetAvailablePairsUseCase(swapRepository: OldSwapRepository): GetAvailablePairsUseCase {
        return GetAvailablePairsUseCase(swapRepository = swapRepository)
    }

    @Provides
    @Singleton
    fun provideGetSwapSupportedPairsUseCase(
        swapRepositoryV2: SwapRepositoryV2,
        swapErrorResolver: SwapErrorResolver,
    ): GetSwapSupportedPairsUseCase {
        return GetSwapSupportedPairsUseCase(
            swapRepositoryV2 = swapRepositoryV2,
            swapErrorResolver = swapErrorResolver,
        )
    }
}