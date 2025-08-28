package com.tangem.features.hotwallet.upgradewallet.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.UpgradeWalletComponent
import com.tangem.features.hotwallet.upgradewallet.DefaultUpgradeWalletComponent
import com.tangem.features.hotwallet.upgradewallet.UpgradeWalletModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface UpgradeWalletModule {

    @Binds
    fun bindUpgradeWalletComponentFactory(impl: DefaultUpgradeWalletComponent.Factory): UpgradeWalletComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(UpgradeWalletModel::class)
    fun bindUpgradeWalletModel(model: UpgradeWalletModel): Model
}