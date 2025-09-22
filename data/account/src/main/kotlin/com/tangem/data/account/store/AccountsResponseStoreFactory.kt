package com.tangem.data.account.store

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

typealias AccountsResponseStore = DataStore<GetWalletAccountsResponse?>

/**
 * Factory class for creating and managing instances of [AccountsResponseStore].
 * This class is responsible for creating a [DataStore] for each unique [UserWalletId].
 *
 * @property context     application context used to access the file system
 * @property moshi       moshi instance for JSON serialization and deserialization
 * @property dispatchers coroutine dispatcher provider
 *
[REDACTED_AUTHOR]
 */
internal class AccountsResponseStoreFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    @NetworkMoshi private val moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    @OptIn(ExperimentalStdlibApi::class)
    private val adapter by lazy { moshi.adapter<GetWalletAccountsResponse?>() }

    private val createdDataStores = ConcurrentHashMap<UserWalletId, AccountsResponseStore>()

    /**
     * Creates or retrieves an [AccountsResponseStore] for the given [UserWalletId].
     *
     * @param userWalletId the unique identifier of the user's wallet
     */
    fun create(userWalletId: UserWalletId): AccountsResponseStore {
        return createdDataStores.computeIfAbsent(userWalletId) {
            DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(defaultValue = null, adapter = adapter),
                produceFile = { context.dataStoreFile(fileName = "wallet_accounts_${userWalletId.stringValue}") },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            )
        }
    }

    @VisibleForTesting
    fun getAllStores(): Map<UserWalletId, AccountsResponseStore> = createdDataStores.toMap()

    @VisibleForTesting
    fun clearStores() {
        createdDataStores.clear()
    }
}