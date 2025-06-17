package com.tangem.features.send.v2.feeselector.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.send.v2.feeselector.model.FeeSelectorModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface FeeSelectorModelModule {
    @Binds
    @IntoMap
    @ClassKey(FeeSelectorModel::class)
    fun bindModel(model: FeeSelectorModel): Model
}