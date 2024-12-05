package com.tangem.tap.di.domain

import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("TooManyFunctions")
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

    @Provides
    @Singleton
    fun provideCheckOnrampAvailabilityUseCase(onrampRepository: OnrampRepository): CheckOnrampAvailabilityUseCase {
        return CheckOnrampAvailabilityUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampStatusUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampStatusUseCase {
        return GetOnrampStatusUseCase(
            onrampRepository,
            onrampErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetOnrampCurrencyUseCase(onrampRepository: OnrampRepository): GetOnrampCurrencyUseCase {
        return GetOnrampCurrencyUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampTransactionsUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
    ): GetOnrampTransactionsUseCase {
        return GetOnrampTransactionsUseCase(onrampTransactionRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampTransactionUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
    ): GetOnrampTransactionUseCase {
        return GetOnrampTransactionUseCase(onrampTransactionRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampRemoveTransactionUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
    ): OnrampRemoveTransactionUseCase {
        return OnrampRemoveTransactionUseCase(onrampTransactionRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampSaveTransactionUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
    ): OnrampSaveTransactionUseCase {
        return OnrampSaveTransactionUseCase(onrampTransactionRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampUpdateTransactionStatusUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
    ): OnrampUpdateTransactionStatusUseCase {
        return OnrampUpdateTransactionStatusUseCase(onrampTransactionRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampPaymentMethodsUseCase(onrampRepository: OnrampRepository): GetOnrampPaymentMethodsUseCase {
        return GetOnrampPaymentMethodsUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideClearOnrampCacheUseCase(onrampRepository: OnrampRepository): ClearOnrampCacheUseCase {
        return ClearOnrampCacheUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampFetchQuotesUseCase(onrampRepository: OnrampRepository): OnrampFetchQuotesUseCase {
        return OnrampFetchQuotesUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampQuotesUseCase(
        settingsRepository: SettingsRepository,
        onrampRepository: OnrampRepository,
    ): GetOnrampQuotesUseCase {
        return GetOnrampQuotesUseCase(
            settingsRepository = settingsRepository,
            repository = onrampRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetOnrampSelectedPaymentMethodUseCase(
        onrampRepository: OnrampRepository,
    ): GetOnrampSelectedPaymentMethodUseCase {
        return GetOnrampSelectedPaymentMethodUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampProviderWithQuoteUseCase(
        onrampRepository: OnrampRepository,
    ): GetOnrampProviderWithQuoteUseCase {
        return GetOnrampProviderWithQuoteUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampSaveSelectedPaymentMethod(onrampRepository: OnrampRepository): OnrampSaveSelectedPaymentMethod {
        return OnrampSaveSelectedPaymentMethod(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideGetOnrampRedirectUrlUseCase(
        onrampRepository: OnrampRepository,
        transactionRepository: OnrampTransactionRepository,
    ): GetOnrampRedirectUrlUseCase {
        return GetOnrampRedirectUrlUseCase(onrampRepository, transactionRepository)
    }
}
