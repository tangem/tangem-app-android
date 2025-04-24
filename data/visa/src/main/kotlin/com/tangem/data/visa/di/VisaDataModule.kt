package com.tangem.data.visa.di

import com.tangem.data.visa.DefaultVisaActivationRepository
import com.tangem.data.visa.DefaultVisaAuthRepository
import com.tangem.data.visa.MockVisaRepository
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.domain.visa.repository.VisaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface VisaDataModule {

    @Binds
    @Singleton
    fun bindVisaAuthRepository(repository: DefaultVisaAuthRepository): VisaAuthRepository

    @Binds
    @Singleton
    fun bindVisaActivationRepositoryFactory(
        repository: DefaultVisaActivationRepository.Factory,
    ): VisaActivationRepository.Factory

    // Mocked
    // @Binds
    // @Singleton
    // fun bindVisaActivationRepositoryFactory(
    //     repository: MockVisaActivationRepository.Factory,
    // ): VisaActivationRepository.Factory

    // @Binds
    // fun bindVisaRepository(repository: DefaultVisaRepository): VisaRepository

    // Mocked
    @Binds
    fun bindVisaRepository(repository: MockVisaRepository): VisaRepository
}