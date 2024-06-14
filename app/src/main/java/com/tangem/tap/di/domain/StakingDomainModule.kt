package com.tangem.tap.di.domain

import com.tangem.domain.settings.*
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.GetStakingEntryInfoUseCase
import com.tangem.domain.staking.FetchStakingTokensUseCase
import com.tangem.domain.staking.GetYieldUseCase
import com.tangem.domain.staking.repositories.StakingRepository
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
    fun provideGetStakingAvailabilityUseCase(stakingRepository: StakingRepository): GetStakingAvailabilityUseCase {
        return GetStakingAvailabilityUseCase(
            stakingRepository = stakingRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetStakingEntryInfoUseCase(stakingRepository: StakingRepository): GetStakingEntryInfoUseCase {
        return GetStakingEntryInfoUseCase(
            stakingRepository = stakingRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetYieldUseCase(stakingRepository: StakingRepository): GetYieldUseCase {
        return GetYieldUseCase(
            stakingRepository = stakingRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetStakingTokensUseCase(stakingRepository: StakingRepository): FetchStakingTokensUseCase {
        return FetchStakingTokensUseCase(
            stakingRepository = stakingRepository,
        )
    }
}