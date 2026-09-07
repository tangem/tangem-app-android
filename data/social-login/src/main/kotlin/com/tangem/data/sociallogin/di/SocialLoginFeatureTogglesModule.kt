package com.tangem.data.sociallogin.di

import com.tangem.data.sociallogin.DefaultSocialLoginFeatureToggles
import com.tangem.domain.sociallogin.SocialLoginFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SocialLoginFeatureTogglesModule {

    @Binds
    @Singleton
    fun bindSocialLoginFeatureToggles(impl: DefaultSocialLoginFeatureToggles): SocialLoginFeatureToggles
}