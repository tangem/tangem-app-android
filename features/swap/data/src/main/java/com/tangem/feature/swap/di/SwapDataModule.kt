package com.tangem.feature.swap.di

import com.tangem.datasource.api.oneinch.OneInchApiFactory
import com.tangem.datasource.api.oneinch.OneInchErrorsHandler
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.feature.swap.SwapRepositoryImpl
import com.tangem.feature.swap.converters.ApproveConverter
import com.tangem.feature.swap.converters.QuotesConverter
import com.tangem.feature.swap.converters.SwapConverter
import com.tangem.feature.swap.converters.TokensConverter
import com.tangem.feature.swap.domain.SwapRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SwapDataModule {

    @Provides
    @Singleton
    fun provideSwapRepository(
        tangemTechApi: TangemTechApi,
        oneInchApiFactory: OneInchApiFactory,
        tokensConverter: TokensConverter,
        quotesConverter: QuotesConverter,
        swapConverter: SwapConverter,
        approveConverter: ApproveConverter,
        oneInchErrorsHandler: OneInchErrorsHandler,
        coroutineDispatcher: CoroutineDispatcherProvider,
    ): SwapRepository {
        return SwapRepositoryImpl(
            tangemTechApi = tangemTechApi,
            oneInchApiFactory = oneInchApiFactory,
            tokensConverter = tokensConverter,
            quotesConverter = quotesConverter,
            swapConverter = swapConverter,
            approveConverter = approveConverter,
            oneInchErrorsHandler = oneInchErrorsHandler,
            coroutineDispatcher = coroutineDispatcher,
        )
    }
}
