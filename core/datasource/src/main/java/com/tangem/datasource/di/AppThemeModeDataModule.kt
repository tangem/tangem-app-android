package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.apptheme.AppThemeModeStore
import com.tangem.datasource.local.apptheme.DefaultAppThemeModeStore
import com.tangem.datasource.local.datastore.SharedPreferencesDataStore
import com.tangem.domain.apptheme.model.AppThemeMode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object AppThemeModeDataModule {

    @Provides
    fun provideAppThemeModeStore(@ApplicationContext context: Context, @NetworkMoshi moshi: Moshi): AppThemeModeStore {
        return DefaultAppThemeModeStore(
            dataStore = SharedPreferencesDataStore(
                preferencesName = "app_theme",
                context = context,
                adapter = moshi.adapter(AppThemeMode::class.java),
            ),
        )
    }
}