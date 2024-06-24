package com.tangem.tap.di.domain

import com.tangem.domain.staking.*
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

    @Provides
    @Singleton
    fun provideCreateEnterActionUseCase(stakingRepository: StakingRepository): InitializeStakingProcessUseCase {
        return InitializeStakingProcessUseCase(stakingRepository)
    }
}
