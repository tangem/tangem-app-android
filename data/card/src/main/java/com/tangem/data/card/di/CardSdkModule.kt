package com.tangem.data.card.di

import com.tangem.data.card.sdk.CardSdkLifecycleObserver
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.data.card.sdk.DefaultCardSdkProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CardSdkModule {

    @Binds
    @Singleton
    fun provideCardSdkProvider(defaultCardSdkProvider: DefaultCardSdkProvider): CardSdkProvider

    @Binds
    @Singleton
    fun providerCardSdkLifecycleObserver(defaultCardSdkProvider: DefaultCardSdkProvider): CardSdkLifecycleObserver
}
