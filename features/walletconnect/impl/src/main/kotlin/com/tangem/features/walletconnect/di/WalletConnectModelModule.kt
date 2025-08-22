package com.tangem.features.walletconnect.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.walletconnect.connections.model.*
import com.tangem.features.walletconnect.connections.routing.WcRoutingModel
import com.tangem.features.walletconnect.transaction.model.WcAddNetworkModel
import com.tangem.features.walletconnect.transaction.model.WcSendTransactionModel
import com.tangem.features.walletconnect.transaction.model.WcSignTransactionModel
import com.tangem.features.walletconnect.transaction.model.WcSwitchNetworkModel
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

    @Binds
    @IntoMap
    @ClassKey(WcPairModel::class)
    fun bindWcPairModel(model: WcPairModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcSelectWalletModel::class)
    fun bindWcSelectWalletModel(model: WcSelectWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcSelectNetworksModel::class)
    fun bindWcSelectNetworksModel(model: WcSelectNetworksModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcRoutingModel::class)
    fun bindWcRoutingModel(model: WcRoutingModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcConnectedAppInfoModel::class)
    fun bindWcConnectedAppInfoModel(model: WcConnectedAppInfoModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcSignTransactionModel::class)
    fun bindWcSignTransactionModel(model: WcSignTransactionModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcAddNetworkModel::class)
    fun bindWcAddNetworkModel(model: WcAddNetworkModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcSwitchNetworkModel::class)
    fun bindWcSwitchNetworkModel(model: WcSwitchNetworkModel): Model

    @Binds
    @IntoMap
    @ClassKey(WcSendTransactionModel::class)
    fun bindWcSendTransactionModel(model: WcSendTransactionModel): Model
}