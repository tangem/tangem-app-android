package com.tangem.data.staking.di

import com.tangem.data.staking.DefaultStakingRepository
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StakingDataModule {

    @Provides
    @Singleton
    fun provideStakingRepository(
        stakeKitApi: StakeKitApi,
        stakingFeatureToggles: StakingFeatureToggles,
    ): StakingRepository {
        return DefaultStakingRepository(
            stakeKitApi = stakeKitApi,
            stakingFeatureToggles = stakingFeatureToggles,
        )
    }
}
