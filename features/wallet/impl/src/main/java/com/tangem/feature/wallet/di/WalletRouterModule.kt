package com.tangem.feature.wallet.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.feature.wallet.presentation.router.DefaultWalletRouter
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn

@Module
@InstallIn(ModelComponent::class)
internal interface WalletRouterModule {

    @Binds
    @ModelScoped
    fun bindsWalletRouter(defaultWalletRouter: DefaultWalletRouter): InnerWalletRouter
}