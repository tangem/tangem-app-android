package com.tangem.features.hotwallet.walletbackup.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.component.DefaultWalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.model.WalletBackupModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletBackupModule {

    @Binds
    @IntoMap
    @ClassKey(WalletBackupModel::class)
    fun bindWalletBackupModel(model: WalletBackupModel): Model

    @Binds
    @Singleton
    fun bindWalletBackupComponentFactory(factory: DefaultWalletBackupComponent.Factory): WalletBackupComponent.Factory
}