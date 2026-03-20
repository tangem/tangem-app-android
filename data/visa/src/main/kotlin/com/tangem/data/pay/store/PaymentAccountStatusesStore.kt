package com.tangem.data.pay.store

import androidx.datastore.core.DataStore
import com.tangem.data.pay.converter.PaymentAccountStatusDMConverter
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.visa.entity.PaymentAccountStatusDM
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.PaymentAccountStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

internal typealias WalletIdWithPaymentStatus = Map<String, PaymentAccountStatus>
internal typealias WalletIdWithPaymentStatusDM = Map<String, PaymentAccountStatusDM>

/**
 * Store for payment account statuses with dual storage (runtime + persistence).
 *
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store for caching across app restarts
 */
internal class PaymentAccountStatusesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithPaymentStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithPaymentStatusDM>,
    private val scope: AppCoroutineScope,
) {

    init {
        scope.launch {
            try {
                val cachedStatuses = persistenceDataStore.data.firstOrNull() ?: return@launch
                runtimeStore.store(
                    value = cachedStatuses.mapValues { (_, statusDM) ->
                        PaymentAccountStatusDMConverter.convertBack(statusDM)
                    },
                )
            } catch (e: Exception) {
                Timber.e(e, "Error while loading cached payment account statuses")
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<PaymentAccountStatus> {
        return runtimeStore.get().mapNotNull { it[userWalletId.stringValue] }
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): PaymentAccountStatus? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    suspend fun store(userWalletId: UserWalletId, status: PaymentAccountStatus) {
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, status = status) }
            launch { storeInPersistence(userWalletId = userWalletId, status = status) }
        }
    }

    suspend fun contains(userWalletId: UserWalletId): Boolean {
        return runtimeStore.getSyncOrDefault(emptyMap()).containsKey(userWalletId.stringValue)
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, status: PaymentAccountStatus) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = status)
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, status: PaymentAccountStatus) {
        val statusDM = PaymentAccountStatusDMConverter.convert(value = status) ?: return
        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = statusDM)
            }
        }
    }
}