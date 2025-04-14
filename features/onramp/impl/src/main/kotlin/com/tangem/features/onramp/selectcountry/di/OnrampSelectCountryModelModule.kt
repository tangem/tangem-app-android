package com.tangem.features.onramp.selectcountry.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.selectcountry.model.OnrampSelectCountryModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface OnrampSelectCountryModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampSelectCountryModel::class)
    fun bindOnrampSelectCountryModel(model: OnrampSelectCountryModel): Model
}