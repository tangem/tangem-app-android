package com.tangem.google.auth.di

import com.tangem.google.auth.CredentialManagerGoogleIdTokenProvider
import com.tangem.google.auth.GoogleIdTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GoogleAuthModule {

    // No app-wide GoogleAuthorizer binding: its picked account is per Drive scope — see GoogleDriveModule

    @Binds
    @Singleton
    fun bindGoogleIdTokenProvider(impl: CredentialManagerGoogleIdTokenProvider): GoogleIdTokenProvider
}