package com.tangem.datasource.di

import com.tangem.datasource.local.AppPreferenceStorage
import com.tangem.datasource.local.AppPreferenceStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AppPreferenceStorageModule {

    @Binds
    @Singleton
    fun bindAppPreferenceStorage(appPreferenceStorageImpl: AppPreferenceStorageImpl): AppPreferenceStorage
}
