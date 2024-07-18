package com.tangem.features.staking.impl.di

import com.tangem.core.navigation.url.UrlOpener
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
    fun provideStakingRouter(urlOpener: UrlOpener): StakingRouter {
        return DefaultStakingRouter(
            urlOpener = urlOpener,
        )
    }
}
