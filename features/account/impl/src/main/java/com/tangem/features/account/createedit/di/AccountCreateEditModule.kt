package com.tangem.features.account.createedit.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.account.createedit.AccountCreateEditModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AccountCreateEditModule {

    @Binds
    @IntoMap
    @ClassKey(AccountCreateEditModel::class)
    fun bindAccountCreateEditModel(model: AccountCreateEditModel): Model
}