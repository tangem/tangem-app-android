package com.tangem.features.pushnotificationsettings.impl.di

import com.tangem.features.pushnotificationsettings.component.PushNotificationSettingsComponent
import com.tangem.features.pushnotificationsettings.impl.component.DefaultPushNotificationSettingsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PushNotificationSettingsComponentModule {

    @Binds
    @Singleton
    fun bindPushNotificationSettingsComponentFactory(
        factory: DefaultPushNotificationSettingsComponent.Factory,
    ): PushNotificationSettingsComponent.Factory
}