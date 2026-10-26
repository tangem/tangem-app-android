package com.tangem.common.di

import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.di.TangemSdkManagerModule
import com.tangem.tap.domain.sdk.impl.MockTangemSdkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [TangemSdkManagerModule::class]
)
object TestModule {

    @Provides
    @Singleton
    fun provideTangemSdkManager(foregroundActivityProvider: ForegroundActivityProvider): TangemSdkManager {
        return MockTangemSdkManager(foregroundActivityProvider)
    }
}