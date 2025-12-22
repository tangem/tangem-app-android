package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.token.DefaultP2PEthPoolVaultsStore
import com.tangem.datasource.local.token.DefaultStakingActionsStore
import com.tangem.datasource.local.token.DefaultStakingYieldsStore
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.StakingActionsStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.datasource.utils.setTypes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StakingStoreModule {

    @Provides
    @Singleton
    fun provideStakingTokensStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): StakingYieldsStore {
        return DefaultStakingYieldsStore(
            dataStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = listTypes<YieldDTO>(),
                    defaultValue = emptyList(),
                ),
                produceFile = { context.dataStoreFile(fileName = "yields_cache") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
        )
    }

    @Provides
    @Singleton
    fun provideYieldsBalancesPersistenceStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): DataStore<Map<String, Set<YieldBalanceWrapperDTO>>> {
        return DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = mapWithStringKeyTypes(valueTypes = setTypes<YieldBalanceWrapperDTO>()),
                defaultValue = emptyMap(),
            ),
            produceFile = { context.dataStoreFile(fileName = "yield_balances") },
            scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
        )
    }

    @Provides
    @Singleton
    fun provideStakingActionsStore(): StakingActionsStore {
        return DefaultStakingActionsStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideP2PBalancesPersistenceStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): DataStore<Map<String, Set<P2PEthPoolAccountResponse>>> {
        return DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = mapWithStringKeyTypes(valueTypes = setTypes<P2PEthPoolAccountResponse>()),
                defaultValue = emptyMap(),
            ),
            produceFile = { context.dataStoreFile(fileName = "p2p_balances") },
            scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
        )
    }

    @Provides
    @Singleton
    fun provideP2PEthPoolVaultsStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): P2PEthPoolVaultsStore {
        return DefaultP2PEthPoolVaultsStore(
            dataStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = listTypes<P2PEthPoolVault>(),
                    defaultValue = emptyList(),
                ),
                produceFile = { context.dataStoreFile(fileName = "p2p_eth_pool_vaults") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
        )
    }
}