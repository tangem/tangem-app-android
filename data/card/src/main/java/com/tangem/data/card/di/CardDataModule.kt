package com.tangem.data.card.di

import com.tangem.data.card.DefaultCardRepository
import com.tangem.data.card.DefaultCardSdkConfigRepository
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
        preferencesDataSource: PreferencesDataSource,
    ): CardSdkConfigRepository {
        return DefaultCardSdkConfigRepository(
            cardSdkProvider = cardSdkProvider,
            preferencesDataSource = preferencesDataSource,
        )
    }

    @Provides
    @Singleton
    fun provideCardRepository(
        preferencesDataSource: PreferencesDataSource,
        dispatchers: CoroutineDispatcherProvider,
    ): CardRepository {
        return DefaultCardRepository(preferencesDataSource = preferencesDataSource, dispatchers = dispatchers)
    }
}
