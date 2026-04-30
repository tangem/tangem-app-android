package com.tangem.tap.di.core.security

import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.tap.core.security.DefaultDeviceSecurityInfoProvider
import com.tangem.tap.core.security.MockAwareDeviceSecurityInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SecurityMockedModule {

    @Provides
    @Singleton
    fun provideDeviceSecurityInfoProvider(
        apiConfigsManager: ApiConfigsManager,
    ): DeviceSecurityInfoProvider {
        val real = DefaultDeviceSecurityInfoProvider()
        return MockAwareDeviceSecurityInfoProvider(real = real, apiConfigsManager = apiConfigsManager)
    }
}