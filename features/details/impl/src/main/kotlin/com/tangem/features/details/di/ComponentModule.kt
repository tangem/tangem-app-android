package com.tangem.features.details.di

import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.component.impl.DefaultDetailsComponent
import com.tangem.features.details.component.impl.DefaultUserWalletListComponent
import com.tangem.features.details.component.impl.DefaultWalletConnectComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindDetailsComponentFactory(factory: DefaultDetailsComponent.Factory): DetailsComponent.Factory

    @Binds
    @Singleton
    fun bindWalletConnectComponentFactory(
        factory: DefaultWalletConnectComponent.Factory,
    ): WalletConnectComponent.Factory

    @Binds
    @Singleton
    fun bindUserWalletListComponentFactory(
        factory: DefaultUserWalletListComponent.Factory,
    ): UserWalletListComponent.Factory
}
