package com.tangem.tap.di.domain

import com.tangem.domain.notifications.GetApplicationIdUseCase
import com.tangem.domain.notifications.SendPushTokenUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
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
    fun providesGetApplicationIdUseCase(notificationsRepository: NotificationsRepository): GetApplicationIdUseCase =
        GetApplicationIdUseCase(
            notificationsRepository = notificationsRepository,
        )

    @Provides
    @Singleton
    fun providesSendPushTokenUseCase(
        notificationsRepository: NotificationsRepository,
        getApplicationIdUseCase: GetApplicationIdUseCase,
        pushNotificationsTokenProvider: PushNotificationsTokenProvider,
    ): SendPushTokenUseCase = SendPushTokenUseCase(
        notificationsRepository = notificationsRepository,
        getApplicationIdUseCase = getApplicationIdUseCase,
        pushNotificationsTokenProvider = pushNotificationsTokenProvider,
    )
}
