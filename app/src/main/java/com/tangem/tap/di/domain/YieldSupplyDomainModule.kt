package com.tangem.tap.di.domain

import com.tangem.domain.blockaid.BlockAidGasEstimate
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
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
    ): YieldSupplyStartEarningUseCase {
        return YieldSupplyStartEarningUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyStopEarningUseCase(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    ): YieldSupplyStopEarningUseCase {
        return YieldSupplyStopEarningUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
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
    ): YieldSupplyGetContractAddressUseCase {
        return YieldSupplyGetContractAddressUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyGetProtocolBalanceUseCase(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    ): YieldSupplyGetProtocolBalanceUseCase {
        return YieldSupplyGetProtocolBalanceUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
        )
    }
}