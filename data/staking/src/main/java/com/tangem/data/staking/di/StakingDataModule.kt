package com.tangem.data.staking.di

import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.staking.*
import com.tangem.data.staking.DefaultStakingErrorResolver
import com.tangem.data.staking.DefaultStakingRepository
import com.tangem.data.staking.DefaultStakingTransactionHashRepository
import com.tangem.data.staking.converters.error.StakeKitErrorConverter
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.StakingActionsStore
import com.tangem.datasource.local.token.StakingBalanceStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.staking.repositories.*
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
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
        stakingYieldsStore: StakingYieldsStore,
        stakingBalanceStore: StakingBalanceStore,
        cacheRegistry: CacheRegistry,
        dispatchers: CoroutineDispatcherProvider,
        walletManagersFacade: WalletManagersFacade,
        getUserWalletUseCase: GetUserWalletUseCase,
        @NetworkMoshi moshi: Moshi,
    ): StakingRepository {
        return DefaultStakingRepository(
            stakeKitApi = stakeKitApi,
            stakingYieldsStore = stakingYieldsStore,
            stakingBalanceStore = stakingBalanceStore,
            cacheRegistry = cacheRegistry,
            dispatchers = dispatchers,
            walletManagersFacade = walletManagersFacade,
            getUserWalletUseCase = getUserWalletUseCase,
            moshi = moshi,
        )
    }

    @Provides
    @Singleton
    fun provideStakingTransactionHashRepository(
        stakeKitApi: StakeKitApi,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): StakingTransactionHashRepository {
        return DefaultStakingTransactionHashRepository(
            stakeKitApi = stakeKitApi,
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideStakingActionRepository(
        stakingActionsStore: StakingActionsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): StakingActionRepository {
        return DefaultStakingActionRepository(
            stakingActionsStore = stakingActionsStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    internal fun provideStakingErrorResolver(
        @NetworkMoshi moshi: Moshi,
        analyticsEventHandler: AnalyticsEventHandler,
    ): StakingErrorResolver {
        val jsonAdapter = moshi.adapter(StakeKitErrorResponse::class.java)
        return DefaultStakingErrorResolver(
            stakeKitErrorConverter = StakeKitErrorConverter(jsonAdapter),
            analyticsEventHandler = analyticsEventHandler,
        )
    }
}