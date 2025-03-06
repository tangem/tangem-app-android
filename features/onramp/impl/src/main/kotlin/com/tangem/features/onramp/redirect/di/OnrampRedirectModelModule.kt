package com.tangem.features.onramp.redirect.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.redirect.model.OnrampRedirectModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface OnrampRedirectModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampRedirectModel::class)
    fun bindOnrampRedirectModel(model: OnrampRedirectModel): Model
}