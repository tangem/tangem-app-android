package com.tangem.feature.wallet.di

import com.tangem.feature.wallet.presentation.router.DefaultWalletRouter
import com.tangem.features.wallet.navigation.WalletRouter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
internal interface WalletRouterModule {

    @Binds
    @ActivityScoped
    fun bindsWalletRouter(defaultWalletRouter: DefaultWalletRouter): WalletRouter
}