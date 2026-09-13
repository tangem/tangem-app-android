package com.tangem.google.auth.di

import com.tangem.google.auth.GoogleAuthorizer
import com.tangem.google.auth.GoogleIdentityAuthorizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface GoogleAuthModule {

    @Binds
    @Singleton
    fun bindGoogleAuthorizer(impl: GoogleIdentityAuthorizer): GoogleAuthorizer
}