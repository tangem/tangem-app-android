package com.tangem.feature.wallet.child.managetokens.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.wallet.child.managetokens.model.AddAndManageModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AddAndManageModule {

    @Binds
    @IntoMap
    @ClassKey(AddAndManageModel::class)
    fun bindAddAndManageModel(model: AddAndManageModel): Model
}