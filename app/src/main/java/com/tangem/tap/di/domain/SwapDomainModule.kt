package com.tangem.tap.di.domain

import com.tangem.feature.swap.domain.GetAvailablePairsUseCase
import com.tangem.feature.swap.domain.api.SwapRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @author Andrew Khokhlov on 09/11/2024
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SwapDomainModule {

    @Provides
    @Singleton
    fun provideGetAvailablePairsUseCase(swapRepository: SwapRepository): GetAvailablePairsUseCase {
        return GetAvailablePairsUseCase(swapRepository = swapRepository)
    }
}
