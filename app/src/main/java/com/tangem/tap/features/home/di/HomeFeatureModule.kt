package com.tangem.tap.features.home.di

import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.home.DefaultHomeComponent
import com.tangem.tap.features.home.HomeModel
import com.tangem.tap.features.home.api.HomeComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface HomeFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultHomeComponent.Factory): HomeComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(HomeModel::class)
    fun bindModel(model: HomeModel): Model
}