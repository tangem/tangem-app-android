package com.tangem.tap.di.domain

import com.tangem.domain.staking.*
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingPendingTransactionRepository
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.staking.repositories.StakingTransactionHashRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StakingDomainModule {

    @Provides
    @Singleton
    fun provideGetStakingAvailabilityUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetStakingAvailabilityUseCase {
        return GetStakingAvailabilityUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetStakingEntryInfoUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetStakingEntryInfoUseCase {
        return GetStakingEntryInfoUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetYieldUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetYieldUseCase {
        return GetYieldUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetStakingTokensUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): FetchStakingTokensUseCase {
        return FetchStakingTokensUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideFetchStakingYieldBalanceUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): FetchStakingYieldBalanceUseCase {
        return FetchStakingYieldBalanceUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideInitializeStakingProcessUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetStakingTransactionUseCase {
        return GetStakingTransactionUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGasEstimateUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): EstimateGasUseCase {
        return EstimateGasUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSubmitHashUseCase(
        stakingTransactionHashRepository: StakingTransactionHashRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): SubmitHashUseCase {
        return SubmitHashUseCase(
            stakingTransactionHashRepository = stakingTransactionHashRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSaveUnsubmittedHashUseCase(
        stakingTransactionHashRepository: StakingTransactionHashRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): SaveUnsubmittedHashUseCase {
        return SaveUnsubmittedHashUseCase(
            stakingTransactionHashRepository = stakingTransactionHashRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSavePendingTransactionUseCase(
        stakingPendingTransactionRepository: StakingPendingTransactionRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): SavePendingTransactionUseCase {
        return SavePendingTransactionUseCase(
            stakingPendingTransactionRepository = stakingPendingTransactionRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideInvalidatePendingTransactionsUseCase(
        stakingPendingTransactionRepository: StakingPendingTransactionRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): InvalidatePendingTransactionsUseCase {
        return InvalidatePendingTransactionsUseCase(
            stakingPendingTransactionRepository = stakingPendingTransactionRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetPendingTransactionsUseCase(
        stakingPendingTransactionRepository: StakingPendingTransactionRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetStakingPendingTransactionsUseCase {
        return GetStakingPendingTransactionsUseCase(
            stakingPendingTransactionRepository = stakingPendingTransactionRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSendUnsubmittedHashesUseCase(
        stakingTransactionHashRepository: StakingTransactionHashRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): SendUnsubmittedHashesUseCase {
        return SendUnsubmittedHashesUseCase(
            stakingTransactionHashRepository = stakingTransactionHashRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideIsApproveNeededUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): IsApproveNeededUseCase {
        return IsApproveNeededUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetConstructedStakingTransactionUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetConstructedStakingTransactionUseCase {
        return GetConstructedStakingTransactionUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideIsAnyTokenStakedUseCase(
        stakingRepository: StakingRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): IsAnyTokenStakedUseCase {
        return IsAnyTokenStakedUseCase(
            stakingRepository = stakingRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetStakingIntegrationIdUseCase(stakingRepository: StakingRepository): GetStakingIntegrationIdUseCase {
        return GetStakingIntegrationIdUseCase(stakingRepository)
    }
}