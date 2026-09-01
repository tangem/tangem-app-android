package com.tangem.tap.di

import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.tap.attestation.GooglePlayIntegrityAttestationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GoogleAttestationModule {

    @Binds
    @Singleton
    fun bindAttestationProvider(impl: GooglePlayIntegrityAttestationProvider): AttestationProvider
}