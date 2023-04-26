package com.tangem.store.preferences.di

import android.content.Context
import com.tangem.store.preferences.PreferencesStore
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
    fun providePreferencesStore(@ApplicationContext context: Context) = PreferencesStore(context)
}
