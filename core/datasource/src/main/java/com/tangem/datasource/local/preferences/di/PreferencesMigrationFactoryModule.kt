package com.tangem.datasource.local.preferences.di

import com.tangem.datasource.local.preferences.migration.SharedPreferencesKeyMigrationFactory
import com.tangem.datasource.local.preferences.utils.DefaultSharedPreferencesKeyMigrationFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface PreferencesMigrationFactoryModule {

    @Binds
    fun bindSharedPreferencesKeyMigrationFactory(
        impl: DefaultSharedPreferencesKeyMigrationFactory,
    ): SharedPreferencesKeyMigrationFactory
}