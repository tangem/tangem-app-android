package com.tangem.data.card.di

import com.tangem.data.card.TransactionSignerFactory
import com.tangem.data.card.DefaultCardRepository
import com.tangem.data.card.DefaultCardSdkConfigRepository
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.card.repository.CardRepository
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
    fun provideCardSdkConfigRepository(
        cardSdkProvider: CardSdkProvider,
        transactionSignerFactory: TransactionSignerFactory,
    ): CardSdkConfigRepository {
        return DefaultCardSdkConfigRepository(
            cardSdkProvider = cardSdkProvider,
            transactionSignerFactory = transactionSignerFactory,
        )
    }

    @Provides
    @Singleton
    fun provideCardRepository(appPreferencesStore: AppPreferencesStore): CardRepository {
        return DefaultCardRepository(appPreferencesStore = appPreferencesStore)
    }
}