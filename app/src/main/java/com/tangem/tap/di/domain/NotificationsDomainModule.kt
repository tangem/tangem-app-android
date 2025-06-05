package com.tangem.tap.di.domain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.notifications.*
import com.tangem.domain.notifications.repository.NotificationsRepository
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
    fun providesGetApplicationIdUseCase(notificationsRepository: NotificationsRepository): GetApplicationIdUseCase {
        return GetApplicationIdUseCase(
            notificationsRepository = notificationsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesSendPushTokenUseCase(
        notificationsRepository: NotificationsRepository,
        pushNotificationsTokenProvider: PushNotificationsTokenProvider,
    ): SendPushTokenUseCase {
        return SendPushTokenUseCase(
            notificationsRepository = notificationsRepository,
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
    fun provideNotificationsFeatureToggles(featureTogglesManager: FeatureTogglesManager): NotificationsFeatureToggles {
        return DefaultNotificationsFeatureToggles(featureTogglesManager = featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideGetNetworksAvailableForNotifications(
        notificationsRepository: NotificationsRepository,
    ): GetNetworksAvailableForNotificationsUseCase {
        return GetNetworksAvailableForNotificationsUseCase(notificationsRepository = notificationsRepository)
    }
}
