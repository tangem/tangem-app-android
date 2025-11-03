package com.tangem.features.account.details.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.account.details.AccountDetailsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AccountDetailsModule {

    @Binds
    @IntoMap
    @ClassKey(AccountDetailsModel::class)
    fun bindAccountDetailsModel(model: AccountDetailsModel): Model
}