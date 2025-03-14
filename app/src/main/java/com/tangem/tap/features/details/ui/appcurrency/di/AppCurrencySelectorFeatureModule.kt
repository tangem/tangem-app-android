package com.tangem.tap.features.details.ui.appcurrency.di

import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.details.ui.appcurrency.DefaultAppCurrencySelectorComponent
import com.tangem.tap.features.details.ui.appcurrency.api.AppCurrencySelectorComponent
import com.tangem.tap.features.details.ui.appcurrency.model.AppCurrencySelectorModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AppCurrencySelectorFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultAppCurrencySelectorComponent.Factory): AppCurrencySelectorComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AppCurrencySelectorModel::class)
    fun bindModel(model: AppCurrencySelectorModel): Model
}