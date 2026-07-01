package com.tangem.features.commonfeatures.impl.managefunds.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.commonfeatures.impl.managefunds.model.ManageFundsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ManageFundsModelModule {

    @Binds
    @IntoMap
    @ClassKey(ManageFundsModel::class)
    fun manageFundsModel(model: ManageFundsModel): Model
}