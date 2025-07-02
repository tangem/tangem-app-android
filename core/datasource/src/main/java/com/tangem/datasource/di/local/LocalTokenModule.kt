package com.tangem.datasource.di.local

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.DefaultUserTokensResponseStore
import com.tangem.datasource.local.token.UserTokensResponseStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocalTokenModule {

    @Provides
    @Singleton
    fun provideUserTokensResponseStore(appPreferencesStore: AppPreferencesStore): UserTokensResponseStore {
        return DefaultUserTokensResponseStore(appPreferencesStore = appPreferencesStore)
    }
}