package com.tangem.features.account.archived.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.account.archived.ArchivedAccountListModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AccountArchivedModule {

    @Binds
    @IntoMap
    @ClassKey(ArchivedAccountListModel::class)
    fun bindArchivedAccountListModel(model: ArchivedAccountListModel): Model
}