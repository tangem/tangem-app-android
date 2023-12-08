package com.tangem.features.send.impl.di

import com.tangem.core.navigation.ReduxNavController
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
    fun provideSendRouter(reduxNavController: ReduxNavController): SendRouter {
        return DefaultSendRouter(reduxNavController)
    }
}
