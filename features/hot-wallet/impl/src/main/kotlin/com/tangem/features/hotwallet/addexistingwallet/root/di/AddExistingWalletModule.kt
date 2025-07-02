package com.tangem.features.hotwallet.addexistingwallet.root.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.AddExistingWalletComponent
import com.tangem.features.hotwallet.addexistingwallet.root.AddExistingWalletModel
import com.tangem.features.hotwallet.addexistingwallet.root.DefaultAddExistingWalletComponent
import com.tangem.features.hotwallet.addexistingwallet.start.AddExistingWalletStartModel
import com.tangem.features.hotwallet.addexistingwallet.im.port.AddExistingWalletImportModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AddExistingWalletModule

@Module
@InstallIn(SingletonComponent::class)
internal interface AddExistingWalletModuleBinds {

    @Binds
    @Singleton
    fun bindAddExistingWalletComponentFactory(
        impl: DefaultAddExistingWalletComponent.Factory,
    ): AddExistingWalletComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AddExistingWalletModel::class)
    fun bindAddExistingWalletModel(model: AddExistingWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(AddExistingWalletStartModel::class)
    fun bindAddExistingWalletStartModel(model: AddExistingWalletStartModel): Model

    @Binds
    @IntoMap
    @ClassKey(AddExistingWalletImportModel::class)
    fun bindAddExistingWalletImportModel(model: AddExistingWalletImportModel): Model
}