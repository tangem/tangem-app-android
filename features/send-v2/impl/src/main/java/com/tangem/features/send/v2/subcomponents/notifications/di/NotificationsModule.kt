package com.tangem.features.send.v2.subcomponents.notifications.di

import com.tangem.features.send.v2.subcomponents.notifications.DefaultNotificationsUpdateTrigger
import com.tangem.features.send.v2.subcomponents.notifications.NotificationsUpdateTrigger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object NotificationsModule {

    @Provides
    @Singleton
    fun providesNotificationsUpdateTrigger(): NotificationsUpdateTrigger {
        return DefaultNotificationsUpdateTrigger()
    }
}