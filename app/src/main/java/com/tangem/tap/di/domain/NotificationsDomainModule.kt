package com.tangem.tap.di.domain

import com.tangem.domain.notifications.GetApplicationIdUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
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
}