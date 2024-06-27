package com.tangem.features.staking.impl.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.features.staking.impl.featuretoggles.DefaultStakingFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI module provides implementation of [StakingFeatureToggles]
 */
@Module
@InstallIn(SingletonComponent::class)
internal object StakingFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideStakingFeatureToggles(featureTogglesManager: FeatureTogglesManager): StakingFeatureToggles {
        return DefaultStakingFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}