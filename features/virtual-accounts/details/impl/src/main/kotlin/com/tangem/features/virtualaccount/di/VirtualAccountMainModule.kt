package com.tangem.features.virtualaccount.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.virtualaccount.DefaultMoneyAccountFeatureToggles
import com.tangem.features.virtualaccount.MoneyAccountFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object VirtualAccountMainModule {

    @Provides
    @Singleton
    fun provideMoneyAccountFeatureToggles(featureTogglesManager: FeatureTogglesManager): MoneyAccountFeatureToggles {
        return DefaultMoneyAccountFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}