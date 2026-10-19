package com.tangem.data.polymarket.di

import com.tangem.data.polymarket.DefaultPolymarketLiveQuotesRepository
import com.tangem.data.polymarket.quote.PolymarketQuoteStore
import com.tangem.domain.polymarket.PolymarketLiveQuotesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketLiveQuotesModule {

    @Binds
    @Singleton
    fun bindLiveQuotesRepository(impl: DefaultPolymarketLiveQuotesRepository): PolymarketLiveQuotesRepository

    companion object {
        @Provides
        @Singleton
        fun provideQuoteStore(): PolymarketQuoteStore = PolymarketQuoteStore(TimeSource.Monotonic)
    }
}