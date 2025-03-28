package com.tangem.sdk.api.di

import com.tangem.sdk.api.featuretoggles.CardSdkFeatureToggles
import com.tangem.sdk.api.featuretoggles.DefaultCardSdkFeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CardSdkFeatureTogglesModule {

    @Binds
    @Singleton
    fun bindCardSdkFeatureToggles(impl: DefaultCardSdkFeatureToggles): CardSdkFeatureToggles
}