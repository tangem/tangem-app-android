package com.tangem.tap.di.domain

import com.tangem.domain.staking.*
import com.tangem.domain.staking.repositories.*
import com.tangem.domain.staking.single.SingleYieldBalanceFetcher
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("TooManyFunctions")
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
        stakeKitRepository: StakeKitRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetStakingEntryInfoUseCase {
        return GetStakingEntryInfoUseCase(
            stakeKitRepository = stakeKitRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetYieldUseCase(
        stakeKitRepository: StakeKitRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetYieldUseCase {
        return GetYieldUseCase(
            stakeKitRepository = stakeKitRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideFetchActionsUseCase(
        stakeKitRepository: StakeKitRepository,
        stakeKitActionRepository: StakeKitActionRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): FetchActionsUseCase {
        return FetchActionsUseCase(
            stakeKitRepository = stakeKitRepository,
            stakeKitActionRepository = stakeKitActionRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetActionsUseCase(
        stakeKitActionRepository: StakeKitActionRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetActionsUseCase {
        return GetActionsUseCase(
            stakeKitActionRepository = stakeKitActionRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetStakingTokensUseCase(
        stakeKitRepository: StakeKitRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): FetchStakingTokensUseCase {
        return FetchStakingTokensUseCase(
            stakeKitRepository = stakeKitRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideFetchStakingOptionsUseCase(
        stakeKitRepository: StakeKitRepository,
        p2pRepository: P2PEthPoolRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): FetchStakingOptionsUseCase {
        return FetchStakingOptionsUseCase(
            stakeKitRepository = stakeKitRepository,
            p2pRepository = p2pRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideFetchStakingYieldBalanceUseCase(
        singleYieldBalanceFetcher: SingleYieldBalanceFetcher,
        stakingIdFactory: StakingIdFactory,
    ): FetchStakingYieldBalanceUseCase {
        return FetchStakingYieldBalanceUseCase(
            singleYieldBalanceFetcher = singleYieldBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
        )
    }

    @Provides
    @Singleton
    fun provideGetStakingTransactionsUseCase(
        stakeKitRepository: StakeKitRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetStakingTransactionsUseCase {
        return GetStakingTransactionsUseCase(
            stakeKitRepository = stakeKitRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGasEstimateUseCase(
        stakeKitRepository: StakeKitRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): EstimateGasUseCase {
        return EstimateGasUseCase(
            stakeKitRepository = stakeKitRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSubmitHashUseCase(
        stakeKitTransactionHashRepository: StakeKitTransactionHashRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): SubmitHashUseCase {
        return SubmitHashUseCase(
            stakeKitTransactionHashRepository = stakeKitTransactionHashRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSaveUnsubmittedHashUseCase(
        stakeKitTransactionHashRepository: StakeKitTransactionHashRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): SaveUnsubmittedHashUseCase {
        return SaveUnsubmittedHashUseCase(
            stakeKitTransactionHashRepository = stakeKitTransactionHashRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideInvalidatePendingTransactionsUseCase(
        stakingErrorResolver: StakingErrorResolver,
    ): InvalidatePendingTransactionsUseCase {
        return InvalidatePendingTransactionsUseCase(
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideSendUnsubmittedHashesUseCase(
        stakeKitTransactionHashRepository: StakeKitTransactionHashRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): SendUnsubmittedHashesUseCase {
        return SendUnsubmittedHashesUseCase(
            stakeKitTransactionHashRepository = stakeKitTransactionHashRepository,
            stakingErrorResolver = stakingErrorResolver,
        )
    }

    @Provides
    @Singleton
    fun provideGetConstructedStakingTransactionUseCase(
        stakeKitRepository: StakeKitRepository,
        stakingErrorResolver: StakingErrorResolver,
    ): GetConstructedStakingTransactionUseCase {
        return GetConstructedStakingTransactionUseCase(
            stakeKitRepository = stakeKitRepository,
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
    fun provideCheckAccountInitializedUseCase(
        walletManagersFacade: WalletManagersFacade,
    ): CheckAccountInitializedUseCase {
        return CheckAccountInitializedUseCase(walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideGetActionRequirementAmountUseCase(): GetActionRequirementAmountUseCase {
        return GetActionRequirementAmountUseCase()
    }

    @Provides
    @Singleton
    fun provideStakingIdFactory(walletManagersFacade: WalletManagersFacade): StakingIdFactory {
        return StakingIdFactory(walletManagersFacade = walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideStakingApyFlowUseCase(stakingRepository: StakingRepository): StakingAvailabilityListUseCase {
        return StakingAvailabilityListUseCase(stakingRepository)
    }
}