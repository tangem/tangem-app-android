package com.tangem.features.staking.impl.di

import com.tangem.core.navigation.ReduxNavController
import com.tangem.features.staking.impl.navigation.DefaultStakingRouter
import com.tangem.features.staking.api.navigation.StakingRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

/**
 * DI module provides implementation of [StakingRouter]
 */
@Module
@InstallIn(ActivityComponent::class)
internal object StakingRouterModule {

    @Provides
    @ActivityScoped
    fun provideStakingRouter(reduxNavController: ReduxNavController): StakingRouter {
        return DefaultStakingRouter(reduxNavController)
    }
}
