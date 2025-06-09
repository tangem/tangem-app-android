package com.tangem.feature.walletsettings.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.walletsettings.component.impl.model.NetworksAvailableForNotificationsModel
import com.tangem.feature.walletsettings.component.impl.model.RenameWalletModel
import com.tangem.feature.walletsettings.model.WalletSettingsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface WalletSettingsModelModule {

    @Binds
    @IntoMap
    @ClassKey(WalletSettingsModel::class)
    fun bindWalletSettingsModel(model: WalletSettingsModel): Model

    @Binds
    @IntoMap
    @ClassKey(RenameWalletModel::class)
    fun bindRenameWalletModel(model: RenameWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(NetworksAvailableForNotificationsModel::class)
    fun bindNetworksAvailableForNotificationsModel(model: NetworksAvailableForNotificationsModel): Model
}