package com.tangem.feature.learn2earn.data.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.datasource.api.promotion.PromotionApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.di.PromotionOneInch
import com.tangem.feature.learn2earn.data.DefaultLearn2earnRepository
import com.tangem.feature.learn2earn.data.DefaultPreferenceStorage
import com.tangem.feature.learn2earn.data.api.Learn2earnPreferenceStorage
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.feature.learn2earn.data.toggles.DevLearn2earnFeatureToggleManager
import com.tangem.feature.learn2earn.data.toggles.Learn2earnFeatureToggleManager
import com.tangem.feature.learn2earn.data.toggles.ProdLearn2earnFeatureToggleManager
import com.tangem.feature.learn2earn.impl.BuildConfig
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Learn2earnDataModule {

    @Provides
    @Singleton
    fun provideFeatureToggle(featureToggleManager: FeatureTogglesManager): Learn2earnFeatureToggleManager {
        return if (BuildConfig.DEBUG) {
            DevLearn2earnFeatureToggleManager()
        } else {
            ProdLearn2earnFeatureToggleManager(featureToggleManager)
        }
    }

    @Provides
    @Singleton
    fun providePreferenceStorage(@ApplicationContext context: Context): Learn2earnPreferenceStorage {
        return DefaultPreferenceStorage(context)
    }

    @Provides
    @Singleton
    fun provideRepository(
        featureToggleManager: Learn2earnFeatureToggleManager,
        preferenceStorage: Learn2earnPreferenceStorage,
        @NetworkMoshi moshi: Moshi,
        @PromotionOneInch promotionApi: PromotionApi,
        dispatchers: AppCoroutineDispatcherProvider,
    ): Learn2earnRepository {
        return DefaultLearn2earnRepository(
            featureToggleManager = featureToggleManager,
            preferencesStorage = preferenceStorage,
            api = promotionApi,
            dispatchers = dispatchers,
            moshi = moshi,
        )
    }
}