package com.tangem.common.di

import android.content.Context
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.di.TangemSdkManagerModule
import com.tangem.tap.domain.sdk.impl.MockTangemSdkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideTangemSdkManager(
        @ApplicationContext context: Context
    ): TangemSdkManager {
        return MockTangemSdkManager(resources = context.resources)
    }
}