package com.tangem.features.earn.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.earn.api.EarnEntryComponent
import com.tangem.features.earn.api.EarnFeatureToggles
import com.tangem.features.earn.api.overview.EarnOverviewFetcher
import com.tangem.features.earn.api.overview.EarnOverviewSupplier
import com.tangem.features.earn.impl.DefaultEarnEntryComponent
import com.tangem.features.earn.impl.DefaultEarnFeatureToggles
import com.tangem.features.earn.impl.overview.DefaultEarnOverviewFetcher
import com.tangem.features.earn.impl.overview.DefaultEarnOverviewSupplier
import com.tangem.features.earn.impl.overview.DefaultEarnProductSummaryProducer
import com.tangem.features.earn.impl.overview.EarnProductSummaryProducer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnComponentModule {

    @Binds
    @Singleton
    fun bindEntryComponentFactory(impl: DefaultEarnEntryComponent.Factory): EarnEntryComponent.Factory

    @Binds
    fun bindSummaryProducerFactory(impl: DefaultEarnProductSummaryProducer.Factory): EarnProductSummaryProducer.Factory

    @Binds
    fun bindOverviewSupplier(impl: DefaultEarnOverviewSupplier): EarnOverviewSupplier

    @Binds
    fun bindOverviewFetcher(impl: DefaultEarnOverviewFetcher): EarnOverviewFetcher

    companion object {

        @Provides
        @Singleton
        fun provideEarnFeatureToggles(featureTogglesManager: FeatureTogglesManager): EarnFeatureToggles {
            return DefaultEarnFeatureToggles(featureTogglesManager)
        }
    }
}