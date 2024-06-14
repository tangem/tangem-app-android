package com.tangem.feature.wallet.di

import com.tangem.core.navigation.ReduxNavController
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.feature.wallet.presentation.router.DefaultWalletRouter
import com.tangem.features.wallet.navigation.WalletRouter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal object WalletRouterModule {

    @Provides
    @ActivityScoped
    fun provideWalletRouter(reduxNavController: ReduxNavController, urlOpener: UrlOpener): WalletRouter {
        return DefaultWalletRouter(reduxNavController, urlOpener)
    }
}
