package com.tangem.datasource.di

import com.tangem.datasource.info.AndroidAppInfoProvider
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.version.AppVersionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppInfoModule {

    @Singleton
    @Provides
    fun provideAppInfoProvider(appVersionProvider: AppVersionProvider): AppInfoProvider {
        return AndroidAppInfoProvider(appVersionProvider)
    }
}