package com.tangem.data.source.preferences.di

import android.content.Context
import com.tangem.data.source.preferences.PreferencesDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PreferencesStoreModule {

    @Provides
    @Singleton
    fun providePreferencesStore(@ApplicationContext context: Context) = PreferencesDataSource(context)
}