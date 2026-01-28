package com.tangem.tap.di.core.security

import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.tap.core.security.DefaultDeviceSecurityInfoProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SecurityModule {

    @Provides
    @Singleton
    fun provideDeviceSecurityInfoProvider(): DeviceSecurityInfoProvider {
        return DefaultDeviceSecurityInfoProvider()
    }
}