package com.tangem.features.foryou.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.ForYouFeatureToggles
import com.tangem.features.foryou.impl.DefaultForYouComponent
import com.tangem.features.foryou.impl.featuretoggles.DefaultForYouFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ForYouFeatureModule {

    @Provides
    @Singleton
    fun provideForYouFeatureToggles(featureTogglesManager: FeatureTogglesManager): ForYouFeatureToggles {
        return DefaultForYouFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface ForYouComponentModule {

    @Binds
    @Singleton
    fun bindForYouComponent(factory: DefaultForYouComponent.Factory): ForYouComponent.Factory
}