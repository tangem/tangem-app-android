package com.tangem.data.common.locale.di

import com.tangem.data.common.locale.DefaultLocaleProvider
import com.tangem.data.common.locale.LocaleProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocaleProviderModule {

    @Provides
    @Singleton
    fun provideCacheRegistry(): LocaleProvider {
        return DefaultLocaleProvider()
    }
}
