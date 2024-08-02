package com.tangem.features.managetokens.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.managetokens.DefaultManageTokensToggles
import com.tangem.features.managetokens.ManageTokensToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object FeatureModule {

    @Provides
    @Singleton
    fun provideFeatureToggles(featureTogglesManager: FeatureTogglesManager): ManageTokensToggles =
        DefaultManageTokensToggles(featureTogglesManager = featureTogglesManager)
}
