package com.tangem.managetokens.di

import com.tangem.core.navigation.ReduxNavController
import com.tangem.features.managetokens.navigation.ManageTokensRouter
import com.tangem.managetokens.presentation.router.DefaultManageTokensRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object ManageTokensRouterModule {

    @Provides
    @ActivityScoped
    fun provideManageTokensRouter(reduxNavController: ReduxNavController): ManageTokensRouter {
        return DefaultManageTokensRouter(reduxNavController)
    }
}