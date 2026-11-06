package com.tangem.data.moneyaccount.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.moneyaccount.converter.MoneyAccountStatusValueDMConverter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

internal typealias WalletIdWithMoneyAccountStatus = Map<String, AccountStatus.Money>
internal typealias WalletIdWithMoneyAccountStatusDM = Map<String, MoneyAccountStatusValueDM>

internal class MoneyAccountStatusesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithMoneyAccountStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithMoneyAccountStatusDM>,
    private val converter: MoneyAccountStatusValueDMConverter,
    private val userWalletsListRepository: UserWalletsListRepository,
    scope: AppCoroutineScope,
) {
    private val initialLoadJob: Job

    init {
        initialLoadJob = scope.launch {
            try {
                val cachedStatuses = persistenceDataStore.data.firstOrNull() ?: return@launch
                userWalletsListRepository.userWallets.filterNotNull().first()
                runtimeStore.store(value = restore(cachedStatuses))
            } catch (_: Exception) {
                runSuspendCatching { persistenceDataStore.updateData { emptyMap() } }
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<AccountStatus.Money?> {
        return runtimeStore.get()
            .onStart { awaitInitialLoad() }
            .map { it[userWalletId.stringValue] }
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): AccountStatus.Money? {
        awaitInitialLoad()
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    suspend fun store(userWalletId: UserWalletId, status: AccountStatus.Money) {
        awaitInitialLoad()
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, status = status) }
            launch { storeInPersistence(userWalletId = userWalletId, status = status.value) }
        }
    }

    suspend fun contains(userWalletId: UserWalletId): Boolean {
        awaitInitialLoad()
        return runtimeStore.getSyncOrDefault(emptyMap()).containsKey(userWalletId.stringValue)
    }

    suspend fun clear(userWalletIds: List<UserWalletId>) {
        awaitInitialLoad()
        val keys = userWalletIds.map { it.stringValue }.toSet()
        coroutineScope {
            launch { runtimeStore.update(default = emptyMap()) { it - keys } }
            launch { persistenceDataStore.updateData { it - keys } }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, status: AccountStatus.Money) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = status)
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, status: MoneyAccountStatusValue) {
        val statusDM = converter.convert(value = status) ?: return
        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                put(key = userWalletId.stringValue, value = statusDM)
            }
        }
    }

    private suspend fun awaitInitialLoad() = initialLoadJob.join()

    private fun restore(cachedStatuses: WalletIdWithMoneyAccountStatusDM): WalletIdWithMoneyAccountStatus {
        return buildMap {
            cachedStatuses.forEach { (rawUserWalletId, statusDM) ->
                val account = Account.Money(userWalletId = UserWalletId(rawUserWalletId))
                runCatching { converter.convertBack(userWalletId = account.userWalletId, value = statusDM) }
                    .onSuccess { statusValue -> put(rawUserWalletId, AccountStatus.Money(account, statusValue)) }
            }
        }
    }
}