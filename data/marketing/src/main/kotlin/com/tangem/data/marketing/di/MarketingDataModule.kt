package com.tangem.data.marketing.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.marketing.DefaultMarketingRepository
import com.tangem.data.marketing.converter.MarketingCampaignConverter
import com.tangem.data.marketing.featuretoggle.DefaultMarketingFeatureToggles
import com.tangem.data.marketing.store.DefaultMarketingCampaignsCacheStore
import com.tangem.data.marketing.store.DefaultMarketingDismissStore
import com.tangem.data.marketing.store.MarketingCampaignsCacheStore
import com.tangem.data.marketing.store.MarketingDismissStore
import com.tangem.datasource.api.marketing.MarketingApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MarketingDataModule {

    @Provides
    @Singleton
    fun provideMarketingCampaignsCacheStore(appPreferencesStore: AppPreferencesStore): MarketingCampaignsCacheStore =
        DefaultMarketingCampaignsCacheStore(appPreferencesStore)

    @Provides
    @Singleton
    fun provideMarketingDismissStore(appPreferencesStore: AppPreferencesStore): MarketingDismissStore =
        DefaultMarketingDismissStore(appPreferencesStore)

    @Provides
    @Singleton
    fun provideMarketingFeatureToggles(featureTogglesManager: FeatureTogglesManager): MarketingFeatureToggles =
        DefaultMarketingFeatureToggles(featureTogglesManager)

    @Provides
    @Singleton
    fun provideMarketingRepository(
        marketingApi: MarketingApi,
        cacheStore: MarketingCampaignsCacheStore,
        dismissStore: MarketingDismissStore,
        dispatchers: CoroutineDispatcherProvider,
    ): MarketingRepository = DefaultMarketingRepository(
        marketingApi = marketingApi,
        cacheStore = cacheStore,
        dismissStore = dismissStore,
        converter = MarketingCampaignConverter(),
        dispatchers = dispatchers,
    )
}