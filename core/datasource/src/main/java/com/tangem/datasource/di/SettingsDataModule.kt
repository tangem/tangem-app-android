package com.tangem.datasource.di

import android.content.Context
import com.tangem.datasource.local.datastore.BooleanSharedPreferencesDataStore
import com.tangem.datasource.local.datastore.IntSharedPreferencesDataStore
import com.tangem.datasource.local.datastore.LongSharedPreferencesDataStore
import com.tangem.datasource.local.settings.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsDataModule {

    @Provides
    @Singleton
    fun provideAppLaunchCountStore(@ApplicationContext context: Context): AppLaunchCountStore {
        return DefaultAppLaunchCountStore(
            store = IntSharedPreferencesDataStore(preferencesName = "tapPrefs", context = context),
        )
    }

    @Provides
    @Singleton
    fun provideAppRatingShowingCountStore(@ApplicationContext context: Context): AppRatingShowingCountStore {
        return DefaultAppRatingShowingCountStore(
            store = IntSharedPreferencesDataStore(preferencesName = "tapPrefs", context = context),
        )
    }

    @Provides
    @Singleton
    fun provideFundsFoundDateInMillisStore(@ApplicationContext context: Context): FundsFoundDateInMillisStore {
        return DefaultFundsFoundDateInMillisStore(
            store = LongSharedPreferencesDataStore(preferencesName = "tapPrefs", context = context),
        )
    }

    @Provides
    @Singleton
    fun provideUserInteractingStatusStore(@ApplicationContext context: Context): UserInteractingStatusStore {
        return DefaultUserInteractingStatusStore(
            store = BooleanSharedPreferencesDataStore(preferencesName = "tapPrefs", context = context),
        )
    }
}