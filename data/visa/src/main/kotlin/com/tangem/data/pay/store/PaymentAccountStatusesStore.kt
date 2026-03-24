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
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

internal typealias WalletIdWithPaymentStatus = Map<String, AccountStatus.Payment>
internal typealias WalletIdWithPaymentStatusDM = Map<String, PaymentAccountStatusValueDM>

/**
 * Store for payment account statuses with dual storage (runtime + persistence).
 *
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store for caching across app restarts
 */
internal class PaymentAccountStatusesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithPaymentStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithPaymentStatusDM>,
    scope: AppCoroutineScope,
) {

    init {
        scope.launch {
            try {
                val cachedStatuses = persistenceDataStore.data.firstOrNull() ?: return@launch
                runtimeStore.store(
                    value = cachedStatuses.mapValues { (rawUserWalletId, statusDM) ->
                        val account = Account.Payment(userWalletId = UserWalletId(rawUserWalletId))
                        val statusValue = PaymentAccountStatusValueDMConverter.convertBack(value = statusDM)
                        AccountStatus.Payment(account = account, value = statusValue)
                    },
                )
            } catch (e: Exception) {
                TangemLogger.e("Error while loading cached payment account statuses", e)
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<AccountStatus.Payment> {
        return runtimeStore.get().mapNotNull { it[userWalletId.stringValue] }
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): AccountStatus.Payment? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
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
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, status = status) }
            launch { storeInPersistence(userWalletId = userWalletId, status = status.value) }
        }
    }

    suspend fun contains(userWalletId: UserWalletId): Boolean {
        return runtimeStore.getSyncOrDefault(emptyMap()).containsKey(userWalletId.stringValue)
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, status: AccountStatus.Payment) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = status)
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, status: PaymentAccountStatusValue) {
        val statusDM = PaymentAccountStatusValueDMConverter.convert(value = status) ?: return
        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = statusDM)
            }
        }
    }
}