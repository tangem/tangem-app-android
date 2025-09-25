package com.tangem.datasource.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.walletconnect.DefaultWalletConnectStore
import com.tangem.datasource.local.walletconnect.WalletConnectStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.setTypes
import com.tangem.domain.walletconnect.model.WcPendingApprovalSessionDTO
import com.tangem.domain.walletconnect.model.WcSessionDTO
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
object WalletConnectModule {

    @Provides
    @Singleton
    fun provideWalletConnectStore(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): WalletConnectStore {
        val scope = CoroutineScope(context = dispatchers.io + SupervisorJob())
        return DefaultWalletConnectStore(
            persistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = setTypes<WcSessionDTO>(),
                    defaultValue = emptySet(),
                ),
                produceFile = { context.dataStoreFile(fileName = "wallet_connect_sessions") },
                scope = scope,
            ),
            pendingApprovalSessionsStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = setTypes<WcPendingApprovalSessionDTO>(),
                    defaultValue = emptySet(),
                ),
                produceFile = { context.dataStoreFile(fileName = "wallet_connect_pending_approval_sessions") },
                scope = scope,
            ),
        )
    }
}