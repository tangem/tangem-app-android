package com.tangem.tap.di.domain

import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.GetVisaTxDetailsUseCase
import com.tangem.domain.visa.GetVisaTxHistoryUseCase
import com.tangem.domain.visa.repository.VisaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object VisaDomainModule {

    @Provides
    @ViewModelScoped
    fun provideVisaCurrencyUseCase(visaRepository: VisaRepository): GetVisaCurrencyUseCase {
        return GetVisaCurrencyUseCase(visaRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetVisaTxHistoryUseCase(visaRepository: VisaRepository): GetVisaTxHistoryUseCase {
        return GetVisaTxHistoryUseCase(visaRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetVisaTxDetailsUseCase(visaRepository: VisaRepository): GetVisaTxDetailsUseCase {
        return GetVisaTxDetailsUseCase(visaRepository)
    }
}