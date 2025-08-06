package com.tangem.features.account.createedit.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.createedit.AccountCreateEditModel
import com.tangem.features.account.createedit.DefaultAccountCreateEditComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountCreateEditModule {

    @Binds
    fun bindAccountCreateEditComponentFactory(
        impl: DefaultAccountCreateEditComponent.Factory,
    ): AccountCreateEditComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AccountCreateEditModel::class)
    fun bindAccountCreateEditModel(model: AccountCreateEditModel): Model
}