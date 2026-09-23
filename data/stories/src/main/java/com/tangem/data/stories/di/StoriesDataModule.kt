package com.tangem.data.stories.di

import com.tangem.data.stories.DefaultStoriesRepository
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.datasource.local.stories.StoriesStore
import com.tangem.domain.stories.StoriesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StoriesDataModule {

    @Provides
    @Singleton
    fun provideStoriesRepository(
        tangemTechApi: TangemTechApi,
        appPreferencesStore: AppPreferencesStore,
        growPreferencesKeys: GrowPreferencesKeys,
        promoStoriesStore: StoriesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): StoriesRepository {
        return DefaultStoriesRepository(
            tangemApi = tangemTechApi,
            appPreferencesStore = appPreferencesStore,
            growPreferencesKeys = growPreferencesKeys,
            promoStoriesStore = promoStoriesStore,
            dispatchers = dispatchers,
        )
    }
}