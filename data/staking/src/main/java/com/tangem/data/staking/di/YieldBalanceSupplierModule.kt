package com.tangem.data.staking.di

import androidx.datastore.core.DataStore
import com.tangem.data.staking.store.DefaultP2PBalancesStore
import com.tangem.data.staking.store.DefaultYieldsBalancesStore
import com.tangem.data.staking.store.P2PBalancesStore
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.domain.staking.multi.MultiYieldBalanceProducer
import com.tangem.domain.staking.multi.MultiYieldBalanceSupplier
import com.tangem.domain.staking.single.SingleYieldBalanceProducer
import com.tangem.domain.staking.single.SingleYieldBalanceSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object YieldBalanceSupplierModule {

    @Provides
    @Singleton
    fun provideYieldsBalancesStore(
        persistenceStore: DataStore<Map<String, Set<YieldBalanceWrapperDTO>>>,
        dispatchers: CoroutineDispatcherProvider,
    ): YieldsBalancesStore {
        return DefaultYieldsBalancesStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceStore = persistenceStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideP2PBalancesStore(
        persistenceStore: DataStore<Map<String, Set<P2PEthPoolAccountResponse>>>,
        p2pVaultsStore: P2PEthPoolVaultsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): P2PBalancesStore {
        return DefaultP2PBalancesStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceStore = persistenceStore,
            vaultsProvider = { runSuspendCatching { p2pVaultsStore.getSync() }.getOrNull().orEmpty() },
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideSingleYieldBalanceSupplier(factory: SingleYieldBalanceProducer.Factory): SingleYieldBalanceSupplier {
        return object : SingleYieldBalanceSupplier(
            factory = factory,
            keyCreator = { params ->
                listOf(
                    "single_yield_balance",
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
    fun provideMultiYieldBalanceSupplier(factory: MultiYieldBalanceProducer.Factory): MultiYieldBalanceSupplier {
        return object : MultiYieldBalanceSupplier(
            factory = factory,
            keyCreator = { "multi_yields_balances_${it.userWalletId.stringValue}" },
        ) {}
    }
}