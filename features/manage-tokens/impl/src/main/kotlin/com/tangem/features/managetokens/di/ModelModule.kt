package com.tangem.features.managetokens.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.managetokens.model.ManageTokensModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(ManageTokensModel::class)
    fun provideManageTokensModel(model: ManageTokensModel): Model
}