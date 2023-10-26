package com.tangem.data.settings.di

import com.tangem.data.settings.DefaultAppRatingRepository
import com.tangem.data.settings.DefaultSettingsRepository
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.datasource.local.settings.AppLaunchCountStore
import com.tangem.datasource.local.settings.AppRatingShowingCountStore
import com.tangem.datasource.local.settings.FundsFoundDateInMillisStore
import com.tangem.datasource.local.settings.UserInteractingStatusStore
import com.tangem.domain.settings.repositories.AppRatingRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDataModule {

    @Provides
    @Singleton
    fun provideSettingsRepository(
        preferencesDataSource: PreferencesDataSource,
        dispatchers: CoroutineDispatcherProvider,
    ): SettingsRepository {
        return DefaultSettingsRepository(
            preferencesDataSource = preferencesDataSource,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideAppRatingRepository(
        fundsFoundDateInMillisStore: FundsFoundDateInMillisStore,
        appLaunchCountStore: AppLaunchCountStore,
        appRatingShowingCountStore: AppRatingShowingCountStore,
        userInteractingStatusStore: UserInteractingStatusStore,
        dispatchers: CoroutineDispatcherProvider,
    ): AppRatingRepository {
        return DefaultAppRatingRepository(
            fundsFoundDateInMillisStore = fundsFoundDateInMillisStore,
            appLaunchCountStore = appLaunchCountStore,
            appRatingShowingCountStore = appRatingShowingCountStore,
            userInteractingStatusStore = userInteractingStatusStore,
            dispatchers = dispatchers,
        )
    }
}