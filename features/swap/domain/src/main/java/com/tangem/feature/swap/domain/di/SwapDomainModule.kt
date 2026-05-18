package com.tangem.feature.swap.domain.di

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.feature.swap.domain.AllowPermissionsHandler
import com.tangem.feature.swap.domain.AllowPermissionsHandlerImpl
import com.tangem.feature.swap.domain.GetSwapUiModeUseCase
import com.tangem.feature.swap.domain.SetSwapUiModeUseCase
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.SwapInteractorImpl
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.feature.swap.domain.transfer.SwapTransferInteractor
import com.tangem.feature.swap.domain.transfer.SwapTransferInteractorImpl
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
        abTestsManager: ABTestsManager,
    ): GetSwapUiModeUseCase = GetSwapUiModeUseCase(
        swapFeatureToggles = swapFeatureToggles,
        swapRepository = swapRepository,
        abTestsManager = abTestsManager,
    )

    @Provides
    @Singleton
    fun provideSetSwapUiModeUseCase(swapRepository: SwapRepository): SetSwapUiModeUseCase =
        SetSwapUiModeUseCase(swapRepository = swapRepository)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapDomainBindModule {

    @Binds
    @Singleton
    fun provideSwapInteractor(swapInteractor: SwapInteractorImpl): SwapInteractor

    @Binds
    @Singleton
    fun provideSwapTransferInteractor(swapTransferInteractor: SwapTransferInteractorImpl): SwapTransferInteractor
}