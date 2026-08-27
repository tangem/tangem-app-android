package com.tangem.core.biometric.impl.di

import com.tangem.core.biometric.BiometricAuthManager
import com.tangem.core.biometric.impl.MockedBiometricAuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface BiometricAuthMockedModule {

    @Binds
    @Singleton
    fun bindBiometricAuthManager(impl: MockedBiometricAuthManager): BiometricAuthManager
}