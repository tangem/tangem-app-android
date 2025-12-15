package com.tangem.features.hotwallet.createhardwarewallet.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.CreateHardwareWalletComponent
import com.tangem.features.hotwallet.createhardwarewallet.CreateHardwareWalletModel
import com.tangem.features.hotwallet.createhardwarewallet.DefaultCreateHardwareWalletComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface CreateHardwareWalletModule {

    @Binds
    fun bindCreateHardwareWalletComponentFactory(
        impl: DefaultCreateHardwareWalletComponent.Factory,
    ): CreateHardwareWalletComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(CreateHardwareWalletModel::class)
    fun bindCreateHardwareWalletModel(model: CreateHardwareWalletModel): Model
}