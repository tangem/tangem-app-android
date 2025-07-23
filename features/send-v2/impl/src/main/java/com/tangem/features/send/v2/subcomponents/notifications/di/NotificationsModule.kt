package com.tangem.features.send.v2.subcomponents.notifications.di

import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.v2.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.send.v2.subcomponents.notifications.DefaultNotificationsUpdateTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface NotificationsModule {

    @Singleton
    @Binds
    fun bindsNotificationsUpdateTrigger(impl: DefaultNotificationsUpdateTrigger): SendNotificationsUpdateTrigger

    @Singleton
    @Binds
    fun bindsNotificationsUpdateListener(impl: DefaultNotificationsUpdateTrigger): SendNotificationsUpdateListener
}