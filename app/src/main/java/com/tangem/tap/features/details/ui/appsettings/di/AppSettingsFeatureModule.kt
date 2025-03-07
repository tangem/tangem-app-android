package com.tangem.tap.features.details.ui.appsettings.di

import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.details.ui.appsettings.DefaultAppSettingsComponent
import com.tangem.tap.features.details.ui.appsettings.api.AppSettingsComponent
import com.tangem.tap.features.details.ui.appsettings.model.AppSettingsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AppSettingsFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultAppSettingsComponent.Factory): AppSettingsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AppSettingsModel::class)
    fun bindModel(model: AppSettingsModel): Model
}