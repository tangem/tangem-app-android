package com.tangem.features.createwalletselection.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.createwalletselection.CreateWalletSelectionComponent
import com.tangem.features.createwalletselection.CreateWalletSelectionModel
import com.tangem.features.createwalletselection.DefaultCreateWalletSelectionComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CreateWalletSelectionModule

@Module
@InstallIn(SingletonComponent::class)
internal interface CreateWalletSelectionModuleBinds {

    @Binds
    @Singleton
    fun bindCreateWalletSelectionComponentFactory(
        impl: DefaultCreateWalletSelectionComponent.Factory,
    ): CreateWalletSelectionComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(CreateWalletSelectionModel::class)
    fun bindCreateWalletSelectionModel(model: CreateWalletSelectionModel): Model
}