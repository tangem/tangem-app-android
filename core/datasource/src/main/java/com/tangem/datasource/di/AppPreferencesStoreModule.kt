package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.preferences.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppPreferencesStoreModule {

    @Provides
    @Singleton
    fun provideAppPreferencesStore(
        @ApplicationContext appContext: Context,
        dispatchers: CoroutineDispatcherProvider,
        @SdkMoshi moshi: Moshi,
    ): AppPreferencesStore {
        return AppPreferencesStore(
            preferencesDataStore = PreferencesDataStore.getInstance(context = appContext, dispatcher = dispatchers.io),
            moshi = moshi,
            dispatchers = dispatchers,
        )
    }
}