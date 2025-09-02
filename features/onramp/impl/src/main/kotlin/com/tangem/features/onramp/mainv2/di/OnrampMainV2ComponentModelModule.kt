package com.tangem.features.onramp.mainv2.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.mainv2.model.OnrampV2MainComponentModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface OnrampMainV2ComponentModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampV2MainComponentModel::class)
    fun bindOnrampSelectCountryModel(model: OnrampV2MainComponentModel): Model
}