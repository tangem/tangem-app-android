package com.tangem.features.pushnotifications.impl.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.pushnotifications.api.featuretoggles.PushNotificationsFeatureToggles
import com.tangem.features.pushnotifications.impl.featuretoggles.DefaultPushNotificationsFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module provides implementation of [PushNotificationsFeatureToggles]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object PushNotificationsFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideSendFeatureToggles(featureTogglesManager: FeatureTogglesManager): PushNotificationsFeatureToggles {
        return DefaultPushNotificationsFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}