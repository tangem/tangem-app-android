package com.tangem.tap.di

import android.content.Context
import com.tangem.domain.card.BuildConfig
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.tap.domain.sdk.impl.DefaultTangemSdkManager
import com.tangem.tap.domain.sdk.impl.MockTangemSdkManager
import com.tangem.tap.domain.sdk.TangemSdkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TangemSdkManagerModule {

    @Provides
    @Singleton
    fun provideTangemSdkManager(
        @ApplicationContext context: Context,
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): TangemSdkManager {
        return if (BuildConfig.MOCK_DATA_SOURCE) {
            MockTangemSdkManager(resources = context.resources)
        } else {
            DefaultTangemSdkManager(cardSdkConfigRepository = cardSdkConfigRepository, resources = context.resources)
        }
    }
}