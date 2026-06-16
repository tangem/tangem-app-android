package com.tangem.data.virtualaccount.store

import androidx.datastore.core.DataStore
import com.tangem.data.virtualaccount.converter.VirtualAccountStatusValueDMConverter
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.visa.entity.VirtualAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal typealias WalletIdWithVirtualStatus = Map<String, AccountStatus.Virtual>
internal typealias WalletIdWithVirtualStatusDM = Map<String, VirtualAccountStatusValueDM>

/**
 * Store for virtual account statuses with dual storage (runtime + persistence).
 *
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store for caching across app restarts
 */
internal class VirtualAccountStatusesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithVirtualStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithVirtualStatusDM>,
    private val converter: VirtualAccountStatusValueDMConverter,
    scope: AppCoroutineScope,
) {

    private val logger = TangemLogger.withTag(TAG)

    init {
        scope.launch {
            try {
                val cachedStatuses = persistenceDataStore.data.firstOrNull() ?: return@launch
                runtimeStore.store(
                    value = cachedStatuses.mapValues { (rawUserWalletId, statusDM) ->
                        val account = Account.Virtual(userWalletId = UserWalletId(rawUserWalletId))
                        val statusValue = converter.convertBack(userWalletId = account.userWalletId, value = statusDM)
                        AccountStatus.Virtual(account = account, value = statusValue)
                    },
                )
            } catch (e: Exception) {
                runSuspendCatching { persistenceDataStore.updateData { emptyMap() } }
                logger.e("Error while loading cached virtual account statuses", e)
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<AccountStatus.Virtual?> {
        return runtimeStore.get()
            .onStart { logger.i("get($userWalletId): subscribed to runtimeStore") }
            .onEach { map ->
                logger.i(
                    "get($userWalletId): runtimeStore emitted map size=${map.size}, " +
                        "hasEntry=${map.containsKey(userWalletId.stringValue)}",
                )
            }
            .map { it[userWalletId.stringValue] }
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): AccountStatus.Virtual? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    suspend fun updateStatusSource(userWalletId: UserWalletId, source: StatusSource) {
        runtimeStore.update(emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val status = this[userWalletId.stringValue] ?: return@update stored
                val newValue = status.copy(value = status.value.copySealed(source = source))
                put(key = userWalletId.stringValue, value = newValue)
            }
        }
    }

    suspend fun store(userWalletId: UserWalletId, status: AccountStatus.Virtual) {
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, status = status) }
            launch { storeInPersistence(userWalletId = userWalletId, status = status.value) }
        }
    }

    suspend fun contains(userWalletId: UserWalletId): Boolean {
        return runtimeStore.getSyncOrDefault(emptyMap()).containsKey(userWalletId.stringValue)
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, status: AccountStatus.Virtual) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = status)
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, status: VirtualAccountStatusValue) {
        val statusDM = converter.convert(value = status) ?: return
        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = statusDM)
            }
        }
    }

    private companion object {
        private const val TAG = "VirtualAccountStatusesStore"
    }
}