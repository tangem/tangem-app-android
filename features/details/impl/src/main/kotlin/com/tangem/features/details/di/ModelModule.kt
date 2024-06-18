package com.tangem.features.details.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.details.model.DetailsModel
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
    @ClassKey(DetailsModel::class)
    fun provideDetailsModel(model: DetailsModel): Model
}