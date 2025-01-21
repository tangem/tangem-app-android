package com.tangem.data.visa.di

import com.tangem.data.visa.DefaultVisaActivationRepository
import com.tangem.data.visa.DefaultVisaAuthRepository
import com.tangem.data.visa.DummyVisaRepository
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.domain.visa.repository.VisaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Singleton
import kotlin.jvm.optionals.getOrNull

@Module
@InstallIn(SingletonComponent::class)
internal object VisaDataModule {

    @Provides
    @Singleton
    fun provideVisaRepository(
        @ImplementedVisaRepository implementedVisaRepository: Optional<VisaRepository>,
    ): VisaRepository {
        return implementedVisaRepository.getOrNull() ?: DummyVisaRepository()
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface VisaDataBindsModule {

    @Binds
    @Singleton
    fun bindVisaAuthRepository(repository: DefaultVisaAuthRepository): VisaAuthRepository

    @Binds
    @Singleton
    fun bindVisaActivationRepositoryFactory(
        repository: DefaultVisaActivationRepository.Factory,
    ): VisaActivationRepository.Factory
}