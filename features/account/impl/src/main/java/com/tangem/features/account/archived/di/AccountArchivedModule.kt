package com.tangem.features.account.archived.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.account.ArchivedAccountListComponent
import com.tangem.features.account.archived.ArchivedAccountListModel
import com.tangem.features.account.archived.DefaultArchivedAccountListComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountArchivedModule {

    @Binds
    fun bindArchivedAccountListComponentFactory(
        impl: DefaultArchivedAccountListComponent.Factory,
    ): ArchivedAccountListComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(ArchivedAccountListModel::class)
    fun bindArchivedAccountListModel(model: ArchivedAccountListModel): Model
}