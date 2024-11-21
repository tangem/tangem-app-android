package com.tangem.data.onramp.di

import com.tangem.data.onramp.DefaultOnrampErrorResolver
import com.tangem.data.onramp.DefaultOnrampRepository
import com.tangem.data.onramp.DefaultOnrampTransactionRepository
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.local.onramp.OnrampPaymentMethodsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnrampDataModule {

    @Provides
    @Singleton
    fun provideOnrampRepository(
        onrampApi: OnrampApi,
        dispatchers: CoroutineDispatcherProvider,
        appPreferencesStore: AppPreferencesStore,
        paymentMethodsStore: OnrampPaymentMethodsStore,
    ): OnrampRepository {
        return DefaultOnrampRepository(
            onrampApi = onrampApi,
            dispatchers = dispatchers,
            appPreferencesStore = appPreferencesStore,
            paymentMethodsStore = paymentMethodsStore,
        )
    }

    @Provides
    @Singleton
    fun provideOnrampTransactionRepository(
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): OnrampTransactionRepository {
        return DefaultOnrampTransactionRepository(
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideOnrampErrorResolver(): OnrampErrorResolver {
        return DefaultOnrampErrorResolver()
    }
}
