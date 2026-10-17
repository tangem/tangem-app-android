package com.tangem.features.hotwallet.sociallogin.di

import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.hotwallet.sociallogin.DefaultSocialLoginUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SocialLoginUseCaseModule {

    @Binds
    @Singleton
    fun bindSocialLoginUseCase(impl: DefaultSocialLoginUseCase): SocialLoginUseCase
}