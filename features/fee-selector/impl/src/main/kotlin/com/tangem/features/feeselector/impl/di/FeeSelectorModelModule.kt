package com.tangem.features.feeselector.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.feeselector.impl.model.FeeSelectorModel
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