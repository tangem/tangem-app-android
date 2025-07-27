package com.tangem.features.hotwallet.addexistingwallet.root.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.AddExistingWalletComponent
import com.tangem.features.hotwallet.addexistingwallet.root.AddExistingWalletModel
import com.tangem.features.hotwallet.addexistingwallet.root.DefaultAddExistingWalletComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AddExistingWalletModule {

    @Binds
    @Singleton
    fun bindAddExistingWalletComponentFactory(
        impl: DefaultAddExistingWalletComponent.Factory,
    ): AddExistingWalletComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AddExistingWalletModel::class)
    fun bindAddExistingWalletModel(model: AddExistingWalletModel): Model
}