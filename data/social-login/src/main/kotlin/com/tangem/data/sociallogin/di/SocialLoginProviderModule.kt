package com.tangem.data.sociallogin.di

import com.tangem.data.sociallogin.provider.BackendSocialLoginNonceProvider
import com.tangem.data.sociallogin.provider.DefaultSocialLoginProviderRepository
import com.tangem.data.sociallogin.provider.SocialLoginNonceProvider
import com.tangem.data.sociallogin.recovery.DefaultSocialRecoveryRepository
import com.tangem.domain.sociallogin.repository.SocialLoginProviderRepository
import com.tangem.domain.sociallogin.repository.SocialRecoveryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SocialLoginProviderModule {

    @Binds
    @Singleton
    fun bindSocialLoginProviderRepository(impl: DefaultSocialLoginProviderRepository): SocialLoginProviderRepository

    @Binds
    @Singleton
    fun bindSocialLoginNonceProvider(impl: BackendSocialLoginNonceProvider): SocialLoginNonceProvider

    @Binds
    @Singleton
    fun bindSocialRecoveryRepository(impl: DefaultSocialRecoveryRepository): SocialRecoveryRepository
}