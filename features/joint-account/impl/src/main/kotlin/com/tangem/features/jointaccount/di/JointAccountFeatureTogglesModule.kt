package com.tangem.features.jointaccount.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.jointaccount.DefaultJointAccountFeatureToggles
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object JointAccountFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideJointAccountFeatureToggles(featureTogglesManager: FeatureTogglesManager): JointAccountFeatureToggles {
        return DefaultJointAccountFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}