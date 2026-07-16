package com.tangem.tap.di.domain

import com.tangem.domain.pushnotificationpreferences.ObserveWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.PreloadWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.SetAllWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.UpdateWalletPushNotificationPreferenceUseCase
import com.tangem.domain.pushnotificationpreferences.repository.WalletPushNotificationPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PushNotificationPreferencesDomainModule {

    @Provides
    @Singleton
    fun providesPreloadWalletPushNotificationPreferencesUseCase(
        repository: WalletPushNotificationPreferencesRepository,
    ): PreloadWalletPushNotificationPreferencesUseCase {
        return PreloadWalletPushNotificationPreferencesUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun providesObserveWalletPushNotificationPreferencesUseCase(
        repository: WalletPushNotificationPreferencesRepository,
    ): ObserveWalletPushNotificationPreferencesUseCase {
        return ObserveWalletPushNotificationPreferencesUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun providesUpdateWalletPushNotificationPreferenceUseCase(
        repository: WalletPushNotificationPreferencesRepository,
    ): UpdateWalletPushNotificationPreferenceUseCase {
        return UpdateWalletPushNotificationPreferenceUseCase(repository = repository)
    }

    @Provides
    @Singleton
    fun providesSetAllWalletPushNotificationPreferencesUseCase(
        repository: WalletPushNotificationPreferencesRepository,
    ): SetAllWalletPushNotificationPreferencesUseCase {
        return SetAllWalletPushNotificationPreferencesUseCase(repository = repository)
    }
}