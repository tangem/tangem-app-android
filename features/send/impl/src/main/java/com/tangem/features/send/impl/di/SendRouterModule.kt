package com.tangem.features.send.impl.di

import com.tangem.common.routing.AppRouter
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.navigation.DefaultSendRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/**
 * DI module provides implementation of [SendRouter]
 */
@Module
@InstallIn(ActivityComponent::class)
internal object SendRouterModule {

    @Provides
    @ActivityScoped
    fun provideSendRouter(appRouter: AppRouter, urlOpener: UrlOpener): SendRouter {
        return DefaultSendRouter(appRouter, urlOpener)
    }
}