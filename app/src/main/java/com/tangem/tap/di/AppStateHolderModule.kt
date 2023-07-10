package com.tangem.tap.di

import com.tangem.core.navigation.NavigationStateHolder
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.tap.proxy.AppStateHolder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppStateHolderModule {

    @Binds
    @Singleton
    fun bindsWalletsStateHolder(appStateHolder: AppStateHolder): WalletsStateHolder

    @Binds
    @Singleton
    fun bindsNavigationStateHolder(appStateHolder: AppStateHolder): NavigationStateHolder
}
