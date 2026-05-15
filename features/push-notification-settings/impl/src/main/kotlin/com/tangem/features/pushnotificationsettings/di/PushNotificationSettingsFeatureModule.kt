package com.tangem.features.pushnotificationsettings.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.pushnotificationsettings.DefaultPushNotificationSettingsFeatureToggles
import com.tangem.features.pushnotificationsettings.PushNotificationSettingsFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PushNotificationSettingsFeatureModule {

    @Provides
    @Singleton
    fun providePushNotificationSettingsFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
    ): PushNotificationSettingsFeatureToggles {
        return DefaultPushNotificationSettingsFeatureToggles(featureTogglesManager)
    }
}