package com.tangem.tap.di.domain

import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.GetVisaTxDetailsUseCase
import com.tangem.domain.visa.GetVisaTxHistoryUseCase
import com.tangem.domain.visa.SetVisaPinCodeUseCase
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object VisaDomainModule {

    @Provides
    fun provideVisaCurrencyUseCase(visaRepository: VisaRepository): GetVisaCurrencyUseCase {
        return GetVisaCurrencyUseCase(visaRepository)
    }

    @Provides
    fun provideGetVisaTxHistoryUseCase(visaRepository: VisaRepository): GetVisaTxHistoryUseCase {
        return GetVisaTxHistoryUseCase(visaRepository)
    }

    @Provides
    fun provideGetVisaTxDetailsUseCase(visaRepository: VisaRepository): GetVisaTxDetailsUseCase {
        return GetVisaTxDetailsUseCase(visaRepository)
    }

    @Provides
    fun provideSetVisaPinCodeUseCase(
        visaActivationRepositoryFactory: VisaActivationRepository.Factory,
    ): SetVisaPinCodeUseCase {
        return SetVisaPinCodeUseCase(visaActivationRepositoryFactory)
    }
}