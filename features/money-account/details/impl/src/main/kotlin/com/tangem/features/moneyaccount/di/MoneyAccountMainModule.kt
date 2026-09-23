package com.tangem.features.moneyaccount.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.moneyaccount.DefaultMoneyAccountFeatureToggles
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MoneyAccountMainModule {

    @Provides
    @Singleton
    fun provideMoneyAccountFeatureToggles(featureTogglesManager: FeatureTogglesManager): MoneyAccountFeatureToggles {
        return DefaultMoneyAccountFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}