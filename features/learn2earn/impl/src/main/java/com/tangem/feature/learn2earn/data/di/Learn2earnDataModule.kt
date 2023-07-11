package com.tangem.feature.learn2earn.data.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.promotion.PromotionApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.di.PromotionOneInch
import com.tangem.feature.learn2earn.data.DefaultLearn2earnRepository
import com.tangem.feature.learn2earn.data.DefaultPreferenceStorage
import com.tangem.feature.learn2earn.data.api.Learn2earnPreferenceStorage
import com.tangem.feature.learn2earn.data.api.Learn2earnRepository
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class Learn2earnDataModule {

    @Provides
    @Singleton
    fun providePreferenceStorage(@ApplicationContext context: Context): Learn2earnPreferenceStorage {
        return DefaultPreferenceStorage(context)
    }

    @Provides
    @Singleton
    fun provideRepository(
        preferenceStorage: Learn2earnPreferenceStorage,
        @NetworkMoshi moshi: Moshi,
        @PromotionOneInch promotionApi: PromotionApi,
        dispatchers: AppCoroutineDispatcherProvider,
    ): Learn2earnRepository {
        return DefaultLearn2earnRepository(
            preferencesStorage = preferenceStorage,
            api = promotionApi,
            dispatchers = dispatchers,
            moshi = moshi,
        )
    }
}
