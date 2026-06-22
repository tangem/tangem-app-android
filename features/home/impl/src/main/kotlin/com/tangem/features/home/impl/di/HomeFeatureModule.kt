package com.tangem.features.home.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.home.api.HomeComponent
import com.tangem.features.home.api.HomeFeatureToggles
import com.tangem.features.home.impl.DefaultHomeComponent
import com.tangem.features.home.impl.DefaultHomeFeatureToggles
import com.tangem.features.home.impl.model.HomeModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindComponent(factory: DefaultHomeComponent.Factory): HomeComponent.Factory
}

@Module
@InstallIn(SingletonComponent::class)
internal object HomeFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideHomeFeatureToggles(featureTogglesManager: FeatureTogglesManager): HomeFeatureToggles {
        return DefaultHomeFeatureToggles(featureTogglesManager)
    }
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(HomeModel::class)
    fun provideModel(model: HomeModel): Model
}