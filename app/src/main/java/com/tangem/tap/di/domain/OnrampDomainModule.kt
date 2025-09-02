package com.tangem.tap.di.domain

import com.tangem.domain.onramp.repositories.LegacyTopUpRepository
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.repositories.HotCryptoRepository
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
    fun provideGetOnrampCurrenciesUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampCurrenciesUseCase {
        return GetOnrampCurrenciesUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideOnrampSaveDefaultCurrencyUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): OnrampSaveDefaultCurrencyUseCase {
        return OnrampSaveDefaultCurrencyUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideGetOnrampCountriesUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampCountriesUseCase {
        return GetOnrampCountriesUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideGetOnrampCountryUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampCountryUseCase {
        return GetOnrampCountryUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideOnrampSaveDefaultCountryUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): OnrampSaveDefaultCountryUseCase {
        return OnrampSaveDefaultCountryUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideCheckOnrampAvailabilityUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): CheckOnrampAvailabilityUseCase {
        return CheckOnrampAvailabilityUseCase(onrampRepository, onrampErrorResolver)
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
    fun provideGetOnrampTransactionsUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampTransactionsUseCase {
        return GetOnrampTransactionsUseCase(onrampTransactionRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideGetOnrampTransactionUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampTransactionUseCase {
        return GetOnrampTransactionUseCase(onrampTransactionRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideOnrampRemoveTransactionUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): OnrampRemoveTransactionUseCase {
        return OnrampRemoveTransactionUseCase(onrampTransactionRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideOnrampSaveTransactionUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): OnrampSaveTransactionUseCase {
        return OnrampSaveTransactionUseCase(onrampTransactionRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideOnrampSepaAvailableUseCase(onrampRepository: OnrampRepository): OnrampSepaAvailableUseCase {
        return OnrampSepaAvailableUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampUpdateTransactionStatusUseCase(
        onrampTransactionRepository: OnrampTransactionRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): OnrampUpdateTransactionStatusUseCase {
        return OnrampUpdateTransactionStatusUseCase(onrampTransactionRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideGetOnrampPaymentMethodsUseCase(
        onrampRepository: OnrampRepository,
        settingsRepository: SettingsRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampPaymentMethodsUseCase {
        return GetOnrampPaymentMethodsUseCase(onrampRepository, settingsRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideClearOnrampCacheUseCase(onrampRepository: OnrampRepository): ClearOnrampCacheUseCase {
        return ClearOnrampCacheUseCase(onrampRepository)
    }

    @Provides
    @Singleton
    fun provideOnrampFetchQuotesUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): OnrampFetchQuotesUseCase {
        return OnrampFetchQuotesUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideGetOnrampQuotesUseCase(
        settingsRepository: SettingsRepository,
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampQuotesUseCase {
        return GetOnrampQuotesUseCase(
            settingsRepository = settingsRepository,
            repository = onrampRepository,
            errorResolver = onrampErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetOnrampProviderWithQuoteUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampProviderWithQuoteUseCase {
        return GetOnrampProviderWithQuoteUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideOnrampFetchPairsUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): OnrampFetchPairsUseCase {
        return OnrampFetchPairsUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideGetOnrampRedirectUrlUseCase(
        onrampRepository: OnrampRepository,
        transactionRepository: OnrampTransactionRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): GetOnrampRedirectUrlUseCase {
        return GetOnrampRedirectUrlUseCase(onrampRepository, transactionRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideFetchOnrampCurrenciesUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): FetchOnrampCurrenciesUseCase {
        return FetchOnrampCurrenciesUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideFetchOnrampCountriesUseCase(
        onrampRepository: OnrampRepository,
        onrampErrorResolver: OnrampErrorResolver,
    ): FetchOnrampCountriesUseCase {
        return FetchOnrampCountriesUseCase(onrampRepository, onrampErrorResolver)
    }

    @Provides
    @Singleton
    fun provideGetHotCryptoTokensUseCase(hotCryptoRepository: HotCryptoRepository): GetHotCryptoUseCase {
        return GetHotCryptoUseCase(hotCryptoRepository)
    }

    @Provides
    @Singleton
    fun provideFetchHotCryptoUseCase(hotCryptoRepository: HotCryptoRepository): FetchHotCryptoUseCase {
        return FetchHotCryptoUseCase(hotCryptoRepository)
    }

    @Provides
    @Singleton
    fun provideGetLegacyTopUpUrlUseCase(legacyTopUpRepository: LegacyTopUpRepository): GetLegacyTopUpUrlUseCase {
        return GetLegacyTopUpUrlUseCase(legacyTopUpRepository)
    }
}