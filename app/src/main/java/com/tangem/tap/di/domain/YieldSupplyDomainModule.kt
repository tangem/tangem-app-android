package com.tangem.tap.di.domain

import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyMarketRepository
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        yieldSupplyMarketRepository: YieldSupplyMarketRepository,
    ): YieldSupplyGetTokenStatusUseCase {
        return YieldSupplyGetTokenStatusUseCase(
            yieldSupplyMarketRepository = yieldSupplyMarketRepository,
        )
    }
}