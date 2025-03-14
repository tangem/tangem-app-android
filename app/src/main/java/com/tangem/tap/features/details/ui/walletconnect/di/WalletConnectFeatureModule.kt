package com.tangem.tap.features.details.ui.walletconnect.di

import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.details.ui.walletconnect.DefaultWalletConnectComponent
import com.tangem.tap.features.details.ui.walletconnect.WalletConnectModel
import com.tangem.tap.features.details.ui.walletconnect.api.WalletConnectComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletConnectFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultWalletConnectComponent.Factory): WalletConnectComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(WalletConnectModel::class)
    fun bindModel(model: WalletConnectModel): Model
}