package com.tangem.features.details.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.details.model.DetailsModel
import com.tangem.features.details.model.UserWalletListModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(DetailsModel::class)
    fun provideDetailsModel(model: DetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(UserWalletListModel::class)
    fun provideUserWalletListModel(model: UserWalletListModel): Model
}