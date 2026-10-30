package com.tangem.tap.di.domain

import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveScoresUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketLiveScoresDomainModule {

    @Provides
    fun provideObservePolymarketSportsStateUseCase(
        repository: PolymarketLiveScoresRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ObservePolymarketSportsStateUseCase = ObservePolymarketSportsStateUseCase(repository, dispatchers)

    @Provides
    @Singleton
    fun provideObservePolymarketLiveScoresUseCase(
        liveScoresRepository: PolymarketLiveScoresRepository,
    ): ObservePolymarketLiveScoresUseCase {
        return ObservePolymarketLiveScoresUseCase(liveScoresRepository = liveScoresRepository)
    }
}