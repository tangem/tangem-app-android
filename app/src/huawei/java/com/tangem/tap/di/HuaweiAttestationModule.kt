package com.tangem.tap.di

import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.lib.auth.attestation.NoAttestationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object HuaweiAttestationModule {

    @Provides
    @Singleton
    fun provideAttestationProvider(): AttestationProvider = NoAttestationProvider
}