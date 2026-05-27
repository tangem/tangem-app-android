package com.tangem.feature.swap.domain.di

import com.tangem.feature.swap.domain.AllowPermissionsHandler
import com.tangem.feature.swap.domain.AllowPermissionsHandlerImpl
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.SwapInteractorImpl
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
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapDomainBindModule {

    @Binds
    @Singleton
    fun provideSwapInteractor(swapInteractor: SwapInteractorImpl): SwapInteractor
}