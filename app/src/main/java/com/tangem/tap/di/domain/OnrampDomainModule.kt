package com.tangem.tap.di.domain

import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.repositories.OnrampRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnrampDomainModule {

    @Provides
    @Singleton
    fun provideGetOnrampCurrenciesUseCase(onrampRepository: OnrampRepository): GetOnrampCurrenciesUseCase {
        return GetOnrampCurrenciesUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampSaveDefaultCurrencyUseCase(onrampRepository: OnrampRepository): OnrampSaveDefaultCurrencyUseCase {
        return OnrampSaveDefaultCurrencyUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampCountriesUseCase(onrampRepository: OnrampRepository): GetOnrampCountriesUseCase {
        return GetOnrampCountriesUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampCountryUseCase(onrampRepository: OnrampRepository): GetOnrampCountryUseCase {
        return GetOnrampCountryUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampSaveDefaultCountryUseCase(onrampRepository: OnrampRepository): OnrampSaveDefaultCountryUseCase {
        return OnrampSaveDefaultCountryUseCase(onrampRepository)
    }
}