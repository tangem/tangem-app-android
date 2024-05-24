package com.tangem.tap.di.domain

import com.tangem.domain.settings.*
import com.tangem.domain.staking.GetStakingEntryInfoUseCase
import com.tangem.domain.staking.repositories.StakingRepository
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
    fun provideGetStakingEntryInfoUseCase(
        stakingRepository: StakingRepository,
    ): GetStakingEntryInfoUseCase {
        return GetStakingEntryInfoUseCase(
            stakingRepository = stakingRepository
        )
    }
}
