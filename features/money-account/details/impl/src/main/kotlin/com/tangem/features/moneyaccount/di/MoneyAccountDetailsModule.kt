package com.tangem.features.moneyaccount.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.moneyaccount.DefaultMoneyAccountFeatureToggles
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.tokens.TokensFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MoneyAccountDetailsModule {

    @Provides
    @Singleton
    fun provideMoneyAccountFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
        tokensFeatureToggles: TokensFeatureToggles,
    ): MoneyAccountFeatureToggles {
        return DefaultMoneyAccountFeatureToggles(
            featureTogglesManager = featureTogglesManager,
            tokensFeatureToggles = tokensFeatureToggles,
        )
    }
}