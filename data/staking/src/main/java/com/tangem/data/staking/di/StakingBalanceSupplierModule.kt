package com.tangem.data.staking.di

import androidx.datastore.core.DataStore
import com.tangem.data.staking.store.DefaultP2PEthPoolBalancesStore
import com.tangem.data.staking.store.DefaultStakingBalancesStore
import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import com.tangem.domain.staking.single.SingleStakingBalanceSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StakingBalanceSupplierModule {

    @Provides
    @Singleton
    fun provideStakingBalancesStore(
        persistenceStore: DataStore<Map<String, Set<YieldBalanceWrapperDTO>>>,
        dispatchers: CoroutineDispatcherProvider,
    ): StakingBalancesStore {
        return DefaultStakingBalancesStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceStore = persistenceStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideP2PEthPoolBalancesStore(
        persistenceStore: DataStore<Map<String, Set<P2PEthPoolAccountResponse>>>,
        dispatchers: CoroutineDispatcherProvider,
    ): P2PEthPoolBalancesStore {
        return DefaultP2PEthPoolBalancesStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceStore = persistenceStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideSingleStakingBalanceSupplier(
        factory: SingleStakingBalanceProducer.Factory,
    ): SingleStakingBalanceSupplier {
        return object : SingleStakingBalanceSupplier(
            factory = factory,
            keyCreator = { params ->
                listOf(
                    "single_staking_balance",
                    params.userWalletId.stringValue,
                    params.stakingId.integrationId,
                    params.stakingId.address,
                )
                    .joinToString(separator = "_")
            },
        ) {}
    }

    @Provides
    @Singleton
    fun provideMultiStakingBalanceSupplier(factory: MultiStakingBalanceProducer.Factory): MultiStakingBalanceSupplier {
        return object : MultiStakingBalanceSupplier(
            factory = factory,
            keyCreator = { "multi_staking_balances_${it.userWalletId.stringValue}" },
        ) {}
    }
}