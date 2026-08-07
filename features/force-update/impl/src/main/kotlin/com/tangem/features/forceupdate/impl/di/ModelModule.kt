package com.tangem.features.forceupdate.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.forceupdate.impl.model.ForceUpdateModel
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
    @ClassKey(ForceUpdateModel::class)
    fun provideForceUpdateModel(model: ForceUpdateModel): Model
}