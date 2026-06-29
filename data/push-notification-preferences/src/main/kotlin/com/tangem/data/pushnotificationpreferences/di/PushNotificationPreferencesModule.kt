package com.tangem.data.pushnotificationpreferences.di

import com.tangem.data.pushnotificationpreferences.DefaultWalletPushNotificationPreferencesRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PushNotificationPreferencesModule {

    @Singleton
    @Provides
    fun providesWalletPushNotificationPreferencesRepository(
        tangemTechApi: TangemTechApi,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletPushNotificationPreferencesRepository = DefaultWalletPushNotificationPreferencesRepository(
        tangemTechApi = tangemTechApi,
        cache = RuntimeSharedStore(),
        dispatchers = dispatchers,
    )
}