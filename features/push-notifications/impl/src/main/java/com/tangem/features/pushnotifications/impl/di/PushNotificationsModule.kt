package com.tangem.features.pushnotifications.impl.di

import com.tangem.common.routing.AppRouter
import com.tangem.features.pushnotifications.api.navigation.PushNotificationsRouter
import com.tangem.features.pushnotifications.impl.navigation.DefaultPushNotificationsRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/**
 * DI module provides implementation of [PushNotificationsRouter]
 */
@Module
@InstallIn(ActivityComponent::class)
object PushNotificationsModule {

    @Provides
    @ActivityScoped
    fun provideDisclaimerRouter(appRouter: AppRouter): PushNotificationsRouter {
        return DefaultPushNotificationsRouter(appRouter)
    }
}