package com.tangem.features.onramp

import com.tangem.core.toggle.feature.FeatureTogglesManager
import com.tangem.features.onramp.DefaultOnrampFeatureToggles
import com.tangem.features.onramp.OnrampFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnrampFeatureModule {

    @Provides
    @Singleton
    fun provideFeatureToggles(featureTogglesManager: FeatureTogglesManager): OnrampFeatureToggles {
        return DefaultOnrampFeatureToggles(featureTogglesManager)
    }
}
