package com.tangem.features.onramp.alloffers.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.alloffers.model.AllOffersModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AllOffersComponentModelModule {

    @Binds
    @IntoMap
    @ClassKey(AllOffersModel::class)
    fun bindAllOffersModel(model: AllOffersModel): Model
}