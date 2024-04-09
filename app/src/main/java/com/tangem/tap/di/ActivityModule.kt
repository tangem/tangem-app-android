package com.tangem.tap.di

import android.content.Context
import com.tangem.domain.card.BuildConfig
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.exchange.RampStateManager
import com.tangem.tap.domain.DefaultTangemSdkManager
import com.tangem.tap.domain.MockTangemSdkManager
import com.tangem.tap.domain.TangemSdkManager
import com.tangem.tap.domain.scanCard.repository.DefaultScanCardRepository
import com.tangem.tap.network.exchangeServices.DefaultRampManager
import com.tangem.tap.proxy.AppStateHolder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        return if (BuildConfig.MOCK_DATA_SOURCE) {
            MockTangemSdkManager(resources = context.resources)
        } else {
            DefaultTangemSdkManager(cardSdkConfigRepository = cardSdkConfigRepository, resources = context.resources)
        }
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
                tangemSdkManager = tangemSdkManager,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideDefaultRampManager(appStateHolder: AppStateHolder): RampStateManager {
        return DefaultRampManager(appStateHolder.exchangeService)
    }

    @Provides
    @Singleton
    @DelayedWork
    fun provideActivityDelayedWorkCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}