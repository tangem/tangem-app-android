package com.tangem.features.hotwallet.wallethardwarebackup.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.WalletHardwareBackupComponent
import com.tangem.features.hotwallet.wallethardwarebackup.component.DefaultWalletHardwareBackupComponent
import com.tangem.features.hotwallet.wallethardwarebackup.model.WalletHardwareBackupModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletHardwareBackupModule

@Module
@InstallIn(SingletonComponent::class)
internal interface WalletHardwareBackupModuleBinds {

    @Binds
    @Singleton
    fun bindWalletHardwareBackupComponentFactory(
        impl: DefaultWalletHardwareBackupComponent.Factory,
    ): WalletHardwareBackupComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(WalletHardwareBackupModel::class)
    fun bindWalletHardwareBackupModel(model: WalletHardwareBackupModel): Model
}