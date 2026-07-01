package com.tangem.data.pay.store

import androidx.datastore.core.DataStore
import com.tangem.data.pay.converter.PaymentAccountStatusValueDMConverter
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal typealias WalletIdWithPaymentStatus = Map<String, AccountStatus.Payment>
internal typealias WalletIdWithPaymentStatusDM = Map<String, PaymentAccountStatusValueDM>

private const val TAG = "PaymentAccountStatusesStore"

/**
 * Store for payment account statuses with dual storage (runtime + persistence).
 *
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store for caching across app restarts
 */
internal class PaymentAccountStatusesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithPaymentStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithPaymentStatusDM>,
    private val converter: PaymentAccountStatusValueDMConverter,
    scope: AppCoroutineScope,
) {

    private val logger = TangemLogger.withTag(TAG)

    init {
        scope.launch {
            logger.i("init: loading cached payment statuses from persistence")
            try {
                val cachedStatuses = persistenceDataStore.data.firstOrNull()
                if (cachedStatuses == null) {
                    logger.i("init: persistence empty (firstOrNull == null), runtimeStore stays empty")
                    return@launch
                }
                logger.i("init: loaded ${cachedStatuses.size} cached entries; populating runtimeStore")
                runtimeStore.store(
                    value = cachedStatuses.mapValues { (rawUserWalletId, statusDM) ->
                        val account = Account.Payment(userWalletId = UserWalletId(rawUserWalletId))
                        val statusValue = converter.convertBack(userWalletId = account.userWalletId, value = statusDM)
                        AccountStatus.Payment(account = account, value = statusValue)
                    },
                )
                logger.i("init: runtimeStore populated with ${cachedStatuses.size} entries")
            } catch (e: Exception) {
                runSuspendCatching { persistenceDataStore.updateData { emptyMap() } }
                logger.e("Error while loading cached payment account statuses", e)
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<AccountStatus.Payment?> {
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

    suspend fun getSyncOrNull(userWalletId: UserWalletId): AccountStatus.Payment? {
        val result = runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
        logger.i("getSyncOrNull($userWalletId) valueType=${result?.value?.let { it::class.simpleName } ?: "null"}")
        return result
    }

    suspend fun updateStatusSource(userWalletId: UserWalletId, source: StatusSource) {
        runtimeStore.update(emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val paymentAccountStatus = this[userWalletId.stringValue] ?: return@update stored
                val newValue = paymentAccountStatus.copy(value = paymentAccountStatus.value.copySealed(source = source))
                put(key = userWalletId.stringValue, value = newValue)
            }
        }
    }

    suspend fun store(userWalletId: UserWalletId, status: AccountStatus.Payment) {
        logger.i("store($userWalletId): valueType=${status.value::class.simpleName}")
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, status = status) }
            launch { storeInPersistence(userWalletId = userWalletId, status = status.value) }
        }
    }

    suspend fun contains(userWalletId: UserWalletId): Boolean {
        return runtimeStore.getSyncOrDefault(emptyMap()).containsKey(userWalletId.stringValue)
    }

    suspend fun remove(userWalletId: UserWalletId) {
        logger.i("remove($userWalletId)")
        remove(userWalletIds = listOf(userWalletId))
    }

    suspend fun remove(userWalletIds: List<UserWalletId>) {
        logger.i("remove($userWalletIds)")
        val keys = userWalletIds.map { it.stringValue }.toSet()
        coroutineScope {
            launch { runtimeStore.update(default = emptyMap()) { it - keys } }
            launch { persistenceDataStore.updateData { it - keys } }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, status: AccountStatus.Payment) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = status)
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, status: PaymentAccountStatusValue) {
        val statusDM = converter.convert(value = status) ?: return
        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = statusDM)
            }
        }
    }
}