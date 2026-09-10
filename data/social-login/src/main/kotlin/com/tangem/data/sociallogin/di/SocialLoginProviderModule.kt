package com.tangem.data.sociallogin.di

import com.tangem.data.sociallogin.provider.DefaultSocialLoginProviderRepository
import com.tangem.data.sociallogin.provider.NoServerNonceProvider
import com.tangem.data.sociallogin.provider.SocialLoginNonceProvider
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SocialLoginProviderModule {

    @Binds
    @Singleton
    fun bindSocialLoginProviderRepository(impl: DefaultSocialLoginProviderRepository): SocialLoginProviderRepository

    companion object {

        @Provides
        @Singleton
        fun provideSocialLoginNonceProvider(): SocialLoginNonceProvider = NoServerNonceProvider()
    }
}