package com.tangem.features.createwalletstart.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.createwalletstart.CreateWalletStartComponent
import com.tangem.features.createwalletstart.CreateWalletStartModel
import com.tangem.features.createwalletstart.DefaultCreateWalletStartComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CreateWalletStartModule

@Module
@InstallIn(SingletonComponent::class)
internal interface CreateWalletStartModuleBinds {

    @Binds
    @Singleton
    fun bindCreateWalletStartComponentFactory(
        impl: DefaultCreateWalletStartComponent.Factory,
    ): CreateWalletStartComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(CreateWalletStartModel::class)
    fun bindCreateWalletStartModel(model: CreateWalletStartModel): Model
}