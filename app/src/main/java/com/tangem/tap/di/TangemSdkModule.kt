package com.tangem.tap.di

import com.tangem.sdk.api.BackupServiceHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemSdkModule {

    @Provides
    @Singleton
    fun provideBackupServiceHolder(): BackupServiceHolder = BackupServiceHolder()
}