package com.tangem.features.walletconnect.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.walletconnect.connections.model.WcConnectionsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface WalletConnectModelModule {

    @Binds
    @IntoMap
    @ClassKey(WcConnectionsModel::class)
    fun bindWcConnectionsModel(model: WcConnectionsModel): Model
}