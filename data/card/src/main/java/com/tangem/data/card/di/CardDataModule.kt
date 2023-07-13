package com.tangem.data.card.di

import com.tangem.data.card.DefaultCardSdkConfigRepository
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.card.repository.CardSdkConfigRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardDataModule {

    @Provides
    @Singleton
    fun provideCardSdkConfigRepository(cardSdkProvider: CardSdkProvider): CardSdkConfigRepository {
        return DefaultCardSdkConfigRepository(cardSdkProvider = cardSdkProvider)
    }
}
