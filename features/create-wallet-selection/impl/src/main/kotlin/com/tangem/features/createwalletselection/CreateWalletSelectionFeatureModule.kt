package com.tangem.features.createwalletselection

import com.tangem.core.decompose.model.Model
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CreateWalletSelectionFeatureModule

@Module
@InstallIn(SingletonComponent::class)
internal interface CreateWalletSelectionFeatureModuleBinds {

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