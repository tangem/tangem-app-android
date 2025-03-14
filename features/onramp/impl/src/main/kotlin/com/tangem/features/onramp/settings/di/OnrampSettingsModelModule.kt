package com.tangem.features.onramp.settings.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.settings.model.OnrampSettingsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface OnrampSettingsModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnrampSettingsModel::class)
    fun bindOnrampSettingsModelModel(model: OnrampSettingsModel): Model
}