package com.tangem.features.account.details.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.account.AccountDetailsComponent
import com.tangem.features.account.details.AccountDetailsModel
import com.tangem.features.account.details.DefaultAccountDetailsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AccountDetailsModule {

    @Binds
    fun bindAccountDetailsComponentFactory(
        impl: DefaultAccountDetailsComponent.Factory,
    ): AccountDetailsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AccountDetailsModel::class)
    fun bindAccountDetailsModel(model: AccountDetailsModel): Model
}