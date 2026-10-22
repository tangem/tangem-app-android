package com.tangem.feature.swap.mcp.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.swap.mcp.DefaultExchangeIntentComponent
import com.tangem.feature.swap.mcp.model.ExchangeIntentModel
import com.tangem.features.swap.mcp.ExchangeIntentComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ExchangeIntentComponentModule {

    @Binds
    @Singleton
    fun bindExchangeIntentComponentFactory(
        impl: DefaultExchangeIntentComponent.Factory,
    ): ExchangeIntentComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ExchangeIntentModelModule {

    @Binds
    @IntoMap
    @ClassKey(ExchangeIntentModel::class)
    fun provideExchangeIntentModel(model: ExchangeIntentModel): Model
}