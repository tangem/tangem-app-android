package com.tangem.tap.di.domain

import com.tangem.domain.visa.SetVisaPinCodeUseCase
import com.tangem.domain.visa.repository.VisaActivationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object VisaDomainModule {

    @Provides
    fun provideSetVisaPinCodeUseCase(
        visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    ): SetVisaPinCodeUseCase {
        return SetVisaPinCodeUseCase(visaActivationRepositoryFactory)
    }
}