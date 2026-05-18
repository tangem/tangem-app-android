package com.tangem.features.commonfeatures.impl.addfunds.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.commonfeatures.impl.addfunds.model.AddFundsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AddFundsModelModule {

    @Binds
    @IntoMap
    @ClassKey(AddFundsModel::class)
    fun addFundsModel(model: AddFundsModel): Model
}