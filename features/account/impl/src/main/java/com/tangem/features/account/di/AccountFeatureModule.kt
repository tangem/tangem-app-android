package com.tangem.features.account.di

import com.tangem.features.account.AccountCreateEditComponent
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.ArchivedAccountListComponent
import com.tangem.features.account.archived.DefaultArchivedAccountListComponent
import com.tangem.features.account.createedit.DefaultAccountCreateEditComponent
import com.tangem.features.account.details.DefaultAccountDetailsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountFeatureModule {

    @Binds
    fun bindAccountCreateEditComponentFactory(
        impl: DefaultAccountCreateEditComponent.Factory,
    ): AccountCreateEditComponent.Factory

    @Binds
    fun bindAccountDetailsComponentFactory(
        impl: DefaultAccountDetailsComponent.Factory,
    ): AccountDetailsComponent.Factory

    @Binds
    fun bindArchivedAccountListComponentFactory(
        impl: DefaultArchivedAccountListComponent.Factory,
    ): ArchivedAccountListComponent.Factory
}