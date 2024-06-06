package com.tangem.data.staking.di

import com.tangem.data.staking.DefaultStakingRepository
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
        stakingTokenStore: StakingYieldsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): StakingRepository {
        return DefaultStakingRepository(
            stakeKitApi = stakeKitApi,
            stakingFeatureToggles = stakingFeatureToggles,
            stakingYieldsStore = stakingTokenStore,
            dispatchers = dispatchers,
        )
    }
}