package com.tangem.tap.di

import android.content.Context
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.scanCard.repository.DefaultScanCardRepository
import com.tangem.tap.userTokensRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ActivityModule {

    @Provides
    @Singleton
    fun provideTangemSdkManager(
        @ApplicationContext context: Context,
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): TangemSdkManager {
        return TangemSdkManager(cardSdkConfigRepository = cardSdkConfigRepository, resources = context.resources)
    }

    @Provides
    @Singleton
    fun provideScanCardUseCase(
        tangemSdkManager: TangemSdkManager,
        cardSdkConfigRepository: CardSdkConfigRepository,
    ): ScanCardUseCase {
        return ScanCardUseCase(
            cardSdkConfigRepository = cardSdkConfigRepository,
            scanCardRepository = DefaultScanCardRepository(
                userTokensRepository = userTokensRepository,
                tangemSdkManager = tangemSdkManager,
            ),
        )
    }
}