package com.tangem.data.staking.di

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.staking.DefaultStakingRepository
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.local.token.StakingBalanceStore
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
        stakingTokenStore: StakingYieldsStore,
        stakingBalanceStore: StakingBalanceStore,
        dispatchers: CoroutineDispatcherProvider,
        stakingFeatureToggle: StakingFeatureToggles,
        cacheRegistry: CacheRegistry,
    ): StakingRepository {
        return DefaultStakingRepository(
            stakeKitApi = stakeKitApi,
            stakingYieldsStore = stakingTokenStore,
            stakingBalanceStore = stakingBalanceStore,
            dispatchers = dispatchers,
            cacheRegistry = cacheRegistry,
            stakingFeatureToggle = stakingFeatureToggle,
        )
    }
}