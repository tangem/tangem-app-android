package com.tangem.tap.di.domain

import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.SwapTransactionRepository
import com.tangem.domain.swap.usecase.*
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

    @Provides
    @Singleton
    fun provideGetSwapPairsUseCase(
        swapRepositoryV2: SwapRepositoryV2,
        swapErrorResolver: SwapErrorResolver,
    ): GetSwapPairsUseCase {
        return GetSwapPairsUseCase(
            swapRepositoryV2 = swapRepositoryV2,
            swapErrorResolver = swapErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSelectInitialPairUseCase(
        swapTransactionRepository: SwapTransactionRepository,
    ): SelectInitialPairUseCase {
        return SelectInitialPairUseCase(
            swapTransactionRepository = swapTransactionRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetSwapQuoteUseCase(
        swapRepositoryV2: SwapRepositoryV2,
        swapErrorResolver: SwapErrorResolver,
    ): GetSwapQuoteUseCase {
        return GetSwapQuoteUseCase(
            swapRepositoryV2 = swapRepositoryV2,
            swapErrorResolver = swapErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetSwapDataUseCase(
        swapRepositoryV2: SwapRepositoryV2,
        swapErrorResolver: SwapErrorResolver,
    ): GetSwapDataUseCase {
        return GetSwapDataUseCase(
            swapRepositoryV2 = swapRepositoryV2,
            swapErrorResolver = swapErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSwapTransactionSentUseCase(
        swapRepositoryV2: SwapRepositoryV2,
        swapTransactionRepository: SwapTransactionRepository,
        swapErrorResolver: SwapErrorResolver,
    ): SwapTransactionSentUseCase {
        return SwapTransactionSentUseCase(
            swapRepositoryV2 = swapRepositoryV2,
            swapTransactionRepository = swapTransactionRepository,
            swapErrorResolver = swapErrorResolver,
        )
    }
}