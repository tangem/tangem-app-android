package com.tangem.core.abtests.di

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.abtests.manager.impl.MockedABTestsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ABTestsManagerMockedModule {

    @Provides
    @Singleton
    fun provideABTestsManager(): ABTestsManager = MockedABTestsManager()
}