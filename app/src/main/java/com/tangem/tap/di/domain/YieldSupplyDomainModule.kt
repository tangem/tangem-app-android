package com.tangem.tap.di.domain

import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStartEarningUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
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
    ): YieldSupplyEstimateEnterFeeUseCase {
        return YieldSupplyEstimateEnterFeeUseCase(
            feeRepository = feeRepository,
            feeErrorResolver = feeErrorResolver,
        )
    }
}