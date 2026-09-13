package com.tangem.tap.features.cloudbackup.di

import com.tangem.data.cloudbackup.datasource.GoogleDriveAuthorizer
import com.tangem.tap.features.cloudbackup.GoogleIdentityDriveAuthorizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CloudBackupAuthModule {

    @Binds
    @Singleton
    fun bindGoogleDriveAuthorizer(impl: GoogleIdentityDriveAuthorizer): GoogleDriveAuthorizer
}