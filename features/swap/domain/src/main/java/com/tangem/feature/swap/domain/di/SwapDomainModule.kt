package com.tangem.feature.swap.domain.di

import com.tangem.feature.swap.domain.AllowPermissionsHandler
import com.tangem.feature.swap.domain.AllowPermissionsHandlerImpl
import com.tangem.feature.swap.domain.GetSwapUiModeUseCase
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.SwapInteractorImpl
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.features.swap.SwapFeatureToggles
import dagger.Binds
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
    fun provideGetSwapUiModeUseCase(
        swapFeatureToggles: SwapFeatureToggles,
        swapRepository: SwapRepository,
    ): GetSwapUiModeUseCase = GetSwapUiModeUseCase(
        swapFeatureToggles = swapFeatureToggles,
        swapRepository = swapRepository,
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapDomainBindModule {

    @Binds
    @Singleton
    fun provideSwapInteractor(swapInteractor: SwapInteractorImpl): SwapInteractor
}