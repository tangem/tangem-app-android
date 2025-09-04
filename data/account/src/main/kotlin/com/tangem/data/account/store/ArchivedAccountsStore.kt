package com.tangem.data.account.store

import androidx.annotation.VisibleForTesting
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.account.models.ArchivedAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.seconds

/**
 * Store for managing archived accounts with support for data expiration
 *
 * @property runtimeStore the underlying runtime shared store for storing the list of archived accounts
 *
[REDACTED_AUTHOR]
 */
internal class ArchivedAccountsStore(
    private val runtimeStore: RuntimeStateStore<List<ArchivedAccount>?>,
) {

    private var timestamp: Long? = null

    /** Retrieves a flow of archived accounts, filtering out null values */
    fun get(): Flow<List<ArchivedAccount>> {
        return runtimeStore.get()
            .map {
                if (isDataExpired()) null else it
            }
            .filterNotNull()
    }

    /** Retrieves the list of archived accounts synchronously, or null if the data is expired */
    suspend fun getSyncOrNull(): List<ArchivedAccount>? {
        if (isDataExpired()) return null

        return runtimeStore.getSyncOrNull()
    }

    /** Stores the provided list of archived accounts [value] */
    suspend fun store(value: List<ArchivedAccount>) {
        timestamp = System.currentTimeMillis()

        runtimeStore.store(value)
    }

    private fun isDataExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val storedTime = timestamp ?: return true

        return currentTime - storedTime >= EXPIRATION_DURATION_MS
    }

    @VisibleForTesting
    fun setTimestamp(time: Long) {
        timestamp = time
    }

    @VisibleForTesting
    fun clear() {
        timestamp = null
        runtimeStore.clear()
    }

    private companion object Companion {
        val EXPIRATION_DURATION_MS = 120.seconds.inWholeMicroseconds
    }
}