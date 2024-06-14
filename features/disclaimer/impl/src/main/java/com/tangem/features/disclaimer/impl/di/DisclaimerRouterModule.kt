package com.tangem.features.disclaimer.impl.di

import com.tangem.common.routing.AppRouter
import com.tangem.features.disclaimer.api.DisclaimerRouter
import com.tangem.features.disclaimer.impl.navigation.DefaultDisclaimerRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/**
 * DI module provides implementation of [DisclaimerRouter]
 */
@Module
@InstallIn(ActivityComponent::class)
object DisclaimerRouterModule {

    @Provides
    @ActivityScoped
    fun provideDisclaimerRouter(appRouter: AppRouter): DisclaimerRouter {
        return DefaultDisclaimerRouter(appRouter)
    }
}