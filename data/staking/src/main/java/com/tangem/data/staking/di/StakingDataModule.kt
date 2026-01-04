package com.tangem.data.staking.di

import com.squareup.moshi.Moshi
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.staking.*
import com.tangem.data.staking.converters.error.StakeKitErrorConverter
import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.data.staking.toggles.DefaultStakingFeatureToggles
import com.tangem.data.staking.utils.DefaultStakingCleaner
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.response.model.error.StakeKitErrorResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.StakingActionsStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.staking.repositories.*
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.staking.utils.StakingCleaner
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
    fun provideStakeKitRepository(
        stakeKitApi: StakeKitApi,
        stakingYieldsStore: StakingYieldsStore,
        dispatchers: CoroutineDispatcherProvider,
        walletManagersFacade: WalletManagersFacade,
        @NetworkMoshi moshi: Moshi,
    ): StakeKitRepository {
        return DefaultStakeKitRepository(
            stakeKitApi = stakeKitApi,
            stakingYieldsStore = stakingYieldsStore,
            dispatchers = dispatchers,
            walletManagersFacade = walletManagersFacade,
            moshi = moshi,
        )
    }

    @Provides
    @Singleton
    fun provideStakingRepository(
        stakeKitRepository: StakeKitRepository,
        p2pEthPoolRepository: P2PEthPoolRepository,
        stakingBalancesStore: StakingBalancesStore,
        dispatchers: CoroutineDispatcherProvider,
        getUserWalletUseCase: GetUserWalletUseCase,
        stakingFeatureToggles: StakingFeatureToggles,
        walletManagersFacade: WalletManagersFacade,
    ): StakingRepository {
        return DefaultStakingRepository(
            stakeKitRepository = stakeKitRepository,
            p2pEthPoolRepository = p2pEthPoolRepository,
            stakingBalanceStoreV2 = stakingBalancesStore,
            dispatchers = dispatchers,
            getUserWalletUseCase = getUserWalletUseCase,
            walletManagersFacade = walletManagersFacade,
            stakingFeatureToggles = stakingFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideP2PEthPoolRepository(
        p2pEthPoolApi: P2PEthPoolApi,
        p2pEthPoolVaultsStore: P2PEthPoolVaultsStore,
        dispatchers: CoroutineDispatcherProvider,
        stakingFeatureToggles: StakingFeatureToggles,
    ): P2PEthPoolRepository {
        return DefaultP2PEthPoolRepository(
            p2pEthPoolApi = p2pEthPoolApi,
            p2pEthPoolVaultsStore = p2pEthPoolVaultsStore,
            dispatchers = dispatchers,
            stakingFeatureToggles = stakingFeatureToggles,
        )
    }

    @Provides
    @Singleton
    fun provideStakingTransactionHashRepository(
        stakeKitApi: StakeKitApi,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): StakeKitTransactionHashRepository {
        return DefaultStakeKitTransactionHashRepository(
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
    ): StakeKitActionRepository {
        return DefaultStakeKitActionRepository(
            stakingActionsStore = stakingActionsStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideStakingErrorResolver(
        @NetworkMoshi moshi: Moshi,
        analyticsEventHandler: AnalyticsEventHandler,
    ): StakingErrorResolver {
        val jsonAdapter = moshi.adapter(StakeKitErrorResponse::class.java)
        return DefaultStakingErrorResolver(
            stakeKitErrorConverter = StakeKitErrorConverter(jsonAdapter),
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    @Provides
    @Singleton
    fun provideFeatureToggles(featureTogglesManager: FeatureTogglesManager): StakingFeatureToggles {
        return DefaultStakingFeatureToggles(featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideStakingCleaner(
        stakingBalancesStore: StakingBalancesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): StakingCleaner {
        return DefaultStakingCleaner(
            stakingBalancesStore = stakingBalancesStore,
            dispatchers = dispatchers,
        )
    }
}