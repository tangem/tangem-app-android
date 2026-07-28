package com.tangem.data.marketing.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.marketing.DefaultMarketingRepository
import com.tangem.data.marketing.converter.MarketingCampaignConverter
import com.tangem.data.marketing.featuretoggle.DefaultMarketingFeatureToggles
import com.tangem.data.marketing.store.DefaultMarketingCampaignsCacheStore
import com.tangem.data.marketing.store.DefaultMarketingDismissStore
import com.tangem.data.marketing.store.MarketingCampaignsCacheStore
import com.tangem.data.marketing.store.MarketingDismissStore
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.datasource.utils.setTypes
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.marketing.MarketingRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object MarketingDataModule {

    @Provides
    @Singleton
    fun provideMarketingCampaignsCacheStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): MarketingCampaignsCacheStore = DefaultMarketingCampaignsCacheStore(
        dataStore = DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = mapWithStringKeyTypes<MarketingCampaignsCacheEntry>(),
                defaultValue = emptyMap(),
            ),
            produceFile = { context.dataStoreFile(fileName = "marketing_campaigns_cache") },
            scope = appScope,
        ),
    )

    @Provides
    @Singleton
    fun provideMarketingDismissStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): MarketingDismissStore = DefaultMarketingDismissStore(
        dataStore = DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = setTypes<Int>(),
                defaultValue = emptySet(),
            ),
            produceFile = { context.dataStoreFile(fileName = "marketing_dismissed_banner_ids") },
            scope = appScope,
        ),
    )

    @Provides
    @Singleton
    fun provideMarketingFeatureToggles(featureTogglesManager: FeatureTogglesManager): MarketingFeatureToggles =
        DefaultMarketingFeatureToggles(featureTogglesManager)

    @Provides
    @Singleton
    fun provideMarketingRepository(
        tangemTechApi: TangemTechApi,
        cacheStore: MarketingCampaignsCacheStore,
        dismissStore: MarketingDismissStore,
        dispatchers: CoroutineDispatcherProvider,
    ): MarketingRepository = DefaultMarketingRepository(
        tangemTechApi = tangemTechApi,
        cacheStore = cacheStore,
        dismissStore = dismissStore,
        converter = MarketingCampaignConverter(),
        dispatchers = dispatchers,
    )
}