package com.tangem.tap.di.domain

import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.usecase.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Module
@InstallIn(SingletonComponent::class)
internal object YieldSupplyDomainModule {

    @Provides
    @Singleton
    fun provideYieldSupplyStartEarningUseCase(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
        yieldSupplyErrorResolver: YieldSupplyErrorResolver,
    ): YieldSupplyStartEarningUseCase {
        return YieldSupplyStartEarningUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
            yieldSupplyErrorResolver = yieldSupplyErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyStopEarningUseCase(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
        yieldSupplyErrorResolver: YieldSupplyErrorResolver,
    ): YieldSupplyStopEarningUseCase {
        return YieldSupplyStopEarningUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
            yieldSupplyErrorResolver = yieldSupplyErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyEstimateEnterFeeUseCase(
        feeRepository: FeeRepository,
        feeErrorResolver: FeeErrorResolver,
        blockAidGasEstimate: BlockAidGasEstimate,
    ): YieldSupplyEstimateEnterFeeUseCase {
        return YieldSupplyEstimateEnterFeeUseCase(
            feeRepository = feeRepository,
            feeErrorResolver = feeErrorResolver,
            blockAidGasEstimate = blockAidGasEstimate,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetContractAddressUseCase(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
        yieldSupplyErrorResolver: YieldSupplyErrorResolver,
    ): YieldSupplyGetContractAddressUseCase {
        return YieldSupplyGetContractAddressUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
            yieldSupplyErrorResolver = yieldSupplyErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetProtocolBalanceUseCase(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
        yieldSupplyErrorResolver: YieldSupplyErrorResolver,
    ): YieldSupplyGetProtocolBalanceUseCase {
        return YieldSupplyGetProtocolBalanceUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
            yieldSupplyErrorResolver = yieldSupplyErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetTokenStatusUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplyGetTokenStatusUseCase {
        return YieldSupplyGetTokenStatusUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetApyUseCase(yieldSupplyRepository: YieldSupplyRepository): YieldSupplyGetApyUseCase {
        return YieldSupplyGetApyUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetChartUseCase(yieldSupplyRepository: YieldSupplyRepository): YieldSupplyGetChartUseCase {
        return YieldSupplyGetChartUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyIsAvailableUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplyIsAvailableUseCase {
        return YieldSupplyIsAvailableUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyActivateUseCase(yieldSupplyRepository: YieldSupplyRepository): YieldSupplyActivateUseCase {
        return YieldSupplyActivateUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyDeactivateUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplyDeactivateUseCase {
        return YieldSupplyDeactivateUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyMinAmountUseCase(
        feeRepository: FeeRepository,
        quotesRepository: QuotesRepository,
        currenciesRepository: CurrenciesRepository,
    ): YieldSupplyMinAmountUseCase {
        return YieldSupplyMinAmountUseCase(
            feeRepository = feeRepository,
            quotesRepository = quotesRepository,
            currenciesRepository = currenciesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetCurrentFeeUseCase(
        feeRepository: FeeRepository,
        quotesRepository: QuotesRepository,
        currenciesRepository: CurrenciesRepository,
    ): YieldSupplyGetCurrentFeeUseCase {
        return YieldSupplyGetCurrentFeeUseCase(
            feeRepository = feeRepository,
            quotesRepository = quotesRepository,
            currenciesRepository = currenciesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetMaxFeeUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
        quotesRepository: QuotesRepository,
        currenciesRepository: CurrenciesRepository,
    ): YieldSupplyGetMaxFeeUseCase {
        return YieldSupplyGetMaxFeeUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
            quotesRepository = quotesRepository,
            currenciesRepository = currenciesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetRewardsBalanceUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
        dispatcherProvider: CoroutineDispatcherProvider,
    ): YieldSupplyGetRewardsBalanceUseCase {
        return YieldSupplyGetRewardsBalanceUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
            dispatcherProvider = dispatcherProvider,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyEnterStatusUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplyEnterStatusUseCase {
        return YieldSupplyEnterStatusUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyEnterStatusFlowUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplyEnterStatusFlowUseCase {
        return YieldSupplyEnterStatusFlowUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetShouldShowMainPromoUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplyGetShouldShowMainPromoUseCase {
        return YieldSupplyGetShouldShowMainPromoUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplySetShouldShowMainPromoUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplySetShouldShowMainPromoUseCase {
        return YieldSupplySetShouldShowMainPromoUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetDustMinAmountUseCase(): YieldSupplyGetDustMinAmountUseCase {
        return YieldSupplyGetDustMinAmountUseCase()
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetAvailabilityUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
    ): YieldSupplyGetAvailabilityUseCase {
        return YieldSupplyGetAvailabilityUseCase(yieldSupplyRepository)
    }

    @Provides
    @Singleton
    fun provideYieldSupplyPendingProcessorUseCase(
        yieldSupplyRepository: YieldSupplyRepository,
        singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
        dispatcherProvider: CoroutineDispatcherProvider,
    ): YieldSupplyPendingTracker {
        return YieldSupplyPendingTracker(
            yieldSupplyRepository = yieldSupplyRepository,
            singleNetworkStatusFetcher = singleNetworkStatusFetcher,
            coroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io),
        )
    }
}