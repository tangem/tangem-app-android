package com.tangem.features.disclaimer.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.disclaimer.impl.model.DisclaimerModel
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
    @ClassKey(DisclaimerModel::class)
    fun provideDisclaimerModel(model: DisclaimerModel): Model
}