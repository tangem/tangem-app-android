package com.tangem.tap.di.domain

import com.tangem.domain.settings.*
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.GetStakingEntryInfoUseCase
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object StakingDomainModule {

    @Provides
    @ViewModelScoped
    fun provideGetStakingEntryInfoUseCase(stakingRepository: StakingRepository): GetStakingEntryInfoUseCase {
        return GetStakingEntryInfoUseCase(
            stakingRepository = stakingRepository,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideGetStakingAvailabilityUseCase(
        stakingRepository: StakingRepository,
    ): GetStakingAvailabilityUseCase {
        return GetStakingAvailabilityUseCase(
            stakingRepository = stakingRepository,
        )
    }
}
