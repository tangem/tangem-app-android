package com.tangem.tap.di.domain

import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.usecase.*
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Merkl incentives overlay of Yield Mode: rewards, the claim and its follow-up. */
@Module
@InstallIn(SingletonComponent::class)
internal object YieldSupplyIncentivesDomainModule {

    @Provides
    @Singleton
    fun provideGetYieldAprHistoryUseCase(
        yieldIncentivesRepository: YieldIncentivesRepository,
    ): GetYieldAprHistoryUseCase {
        return GetYieldAprHistoryUseCase(
            yieldIncentivesRepository = yieldIncentivesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetFreshYieldClaimCalldataUseCase(
        yieldIncentivesRepository: YieldIncentivesRepository,
    ): GetFreshYieldClaimCalldataUseCase {
        return GetFreshYieldClaimCalldataUseCase(
            yieldIncentivesRepository = yieldIncentivesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideValidateYieldClaimCalldataUseCase(
        yieldModuleAddressProvider: YieldModuleAddressProvider,
    ): ValidateYieldClaimCalldataUseCase {
        return ValidateYieldClaimCalldataUseCase(
            yieldModuleAddressProvider = yieldModuleAddressProvider,
        )
    }

    @Provides
    @Singleton
    fun provideBuildYieldClaimTransactionUseCase(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
        yieldSupplyErrorResolver: YieldSupplyErrorResolver,
    ): BuildYieldClaimTransactionUseCase {
        return BuildYieldClaimTransactionUseCase(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
            yieldSupplyErrorResolver = yieldSupplyErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetYieldRewardsSummaryUseCase(
        yieldIncentivesRepository: YieldIncentivesRepository,
    ): GetYieldRewardsSummaryUseCase {
        return GetYieldRewardsSummaryUseCase(
            yieldIncentivesRepository = yieldIncentivesRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyClaimTracker(
        yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
        yieldIncentivesRepository: YieldIncentivesRepository,
        appScope: AppCoroutineScope,
    ): YieldSupplyClaimTracker {
        return YieldSupplyClaimTracker(
            yieldSupplyTransactionRepository = yieldSupplyTransactionRepository,
            yieldIncentivesRepository = yieldIncentivesRepository,
            coroutineScope = appScope,
        )
    }

    @Provides
    @Singleton
    fun provideRefreshYieldIncentivesUseCase(
        yieldIncentivesRepository: YieldIncentivesRepository,
    ): RefreshYieldIncentivesUseCase {
        return RefreshYieldIncentivesUseCase(yieldIncentivesRepository)
    }
}