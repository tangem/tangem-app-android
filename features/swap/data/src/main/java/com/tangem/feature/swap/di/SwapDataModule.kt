package com.tangem.feature.swap.di

import com.tangem.datasource.api.oneinch.OneInchApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.qualifiers.OneInchEthereum
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
        @OneInchEthereum oneInchApi: OneInchApi,
        tokensConverter: TokensConverter,
        quotesConverter: QuotesConverter,
        swapConverter: SwapConverter,
        approveConverter: ApproveConverter,
        coroutineDispatcher: CoroutineDispatcherProvider,
    ): SwapRepository {
        return SwapRepositoryImpl(
            tangemTechApi = tangemTechApi,
            oneInchApi = oneInchApi,
            tokensConverter = tokensConverter,
            quotesConverter = quotesConverter,
            swapConverter = swapConverter,
            approveConverter = approveConverter,
            coroutineDispatcher = coroutineDispatcher,
        )
    }
}
