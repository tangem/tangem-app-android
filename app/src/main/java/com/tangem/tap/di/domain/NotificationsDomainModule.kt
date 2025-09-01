package com.tangem.tap.di.domain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.notifications.*
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.notifications.repository.PushNotificationsRepository
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles
import com.tangem.tap.domain.notifications.DefaultNotificationsFeatureToggles
import com.tangem.utils.notifications.PushNotificationsTokenProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object NotificationsDomainModule {

    @Provides
    @Singleton
    fun providesGetApplicationIdUseCase(
        pushNotificationsRepository: PushNotificationsRepository,
    ): GetApplicationIdUseCase {
        return GetApplicationIdUseCase(
            pushNotificationsRepository = pushNotificationsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesSendPushTokenUseCase(
        pushNotificationsRepository: PushNotificationsRepository,
        pushNotificationsTokenProvider: PushNotificationsTokenProvider,
    ): SendPushTokenUseCase {
        return SendPushTokenUseCase(
            pushNotificationsRepository = pushNotificationsRepository,
            pushNotificationsTokenProvider = pushNotificationsTokenProvider,
        )
    }

    @Provides
    @Singleton
    fun providesGetTronFeeNotificationShowCountUseCase(
        notificationsRepository: NotificationsRepository,
    ): GetTronFeeNotificationShowCountUseCase {
        return GetTronFeeNotificationShowCountUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesIncrementTronFeeNotificationShowCountUseCase(
        notificationsRepository: NotificationsRepository,
    ): IncrementNotificationsShowCountUseCase {
        return IncrementNotificationsShowCountUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesShouldShowNotificationUseCase(
        notificationsRepository: NotificationsRepository,
    ): ShouldShowNotificationUseCase {
        return ShouldShowNotificationUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesSetShouldShowNotificationUseCase(
        notificationsRepository: NotificationsRepository,
    ): SetShouldShowNotificationUseCase {
        return SetShouldShowNotificationUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @Provides
    @Singleton
    fun provideNotificationsFeatureToggles(featureTogglesManager: FeatureTogglesManager): NotificationsFeatureToggles {
        return DefaultNotificationsFeatureToggles(featureTogglesManager = featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideGetNetworksAvailableForNotifications(
        pushNotificationsRepository: PushNotificationsRepository,
    ): GetNetworksAvailableForNotificationsUseCase {
        return GetNetworksAvailableForNotificationsUseCase(pushNotificationsRepository = pushNotificationsRepository)
    }
}