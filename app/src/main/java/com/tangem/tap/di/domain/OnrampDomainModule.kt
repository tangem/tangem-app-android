package com.tangem.tap.di.domain

import com.tangem.domain.onramp.GetOnrampCurrenciesUseCase
import com.tangem.domain.onramp.OnrampSaveDefaultCurrencyUseCase
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
}
