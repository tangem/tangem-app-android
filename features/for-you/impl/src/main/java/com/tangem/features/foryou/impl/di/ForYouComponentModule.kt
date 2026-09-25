package com.tangem.features.foryou.impl.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.impl.DefaultForYouComponent
import com.tangem.features.foryou.impl.model.ForYouModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ForYouComponentModule {

    @Binds
    @Singleton
    fun bindForYouComponent(factory: DefaultForYouComponent.Factory): ForYouComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(ForYouModel::class)
    fun bindForYouModel(impl: ForYouModel): Model
}