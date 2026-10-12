package com.tangem.features.hotwallet.sociallogin.di

import com.tangem.features.hotwallet.SocialLoginFlow
import com.tangem.features.hotwallet.sociallogin.DefaultSocialLoginFlow
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SocialLoginFlowModule {

    @Binds
    @Singleton
    fun bindSocialLoginFlow(impl: DefaultSocialLoginFlow): SocialLoginFlow
}