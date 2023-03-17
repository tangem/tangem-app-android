package com.tangem.feature.swap.di

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.feature.swap.api.SwapFeatureToggleManager
import com.tangem.feature.swap.toggles.DefaultFeatureTogglesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SwapFeatureTogglesManagerModule {

    @Provides
    @ActivityRetainedScoped
    fun provideSwapFeatureTogglesManager(featureTogglesManager: FeatureTogglesManager): SwapFeatureToggleManager {
        return DefaultFeatureTogglesManager(featureTogglesManager)
    }
}
