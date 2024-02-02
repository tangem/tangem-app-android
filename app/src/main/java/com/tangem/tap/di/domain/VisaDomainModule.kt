package com.tangem.tap.di.domain

import com.tangem.domain.visa.GetVisaCurrencyUseCase
import com.tangem.domain.visa.repository.VisaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
internal object VisaDomainModule {

    @Provides
    fun provideVisaCurrencyUseCase(visaRepository: VisaRepository): GetVisaCurrencyUseCase {
        return GetVisaCurrencyUseCase(visaRepository)
    }
}