package com.tangem.managetokens.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.managetokens.featuretoggles.ManageTokensFeatureToggles
import com.tangem.managetokens.featuretoggles.DefaultManageTokensFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ManageTokensFeatureTogglesModule {

    @Provides
    @Singleton
    fun provideWalletFeatureToggles(featureTogglesManager: FeatureTogglesManager): ManageTokensFeatureToggles {
        return DefaultManageTokensFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}