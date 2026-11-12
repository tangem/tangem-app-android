package com.tangem.data.tangempay.store

import androidx.datastore.core.DataStore
import com.tangem.data.tangempay.converter.PaymentAccountStatusValueDMConverter
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.spend.datasource.tangempay.entity.PaymentAccountStatusValueDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.tangempay.TangemPayCardState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.flow.PaymentAccountStatusCache
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
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
    private val userWalletsListRepository: UserWalletsListRepository,
    scope: AppCoroutineScope,
) : PaymentAccountStatusCache {

    private val logger = TangemLogger.withTag(TAG)

    private val initialLoadJob: Job

    init {
        initialLoadJob = scope.launch {
            logger.i("init: loading cached payment statuses from persistence")
            try {
                val cachedStatuses = persistenceDataStore.data.firstOrNull()
                if (cachedStatuses == null) {
                    logger.i("init: persistence empty (firstOrNull == null), runtimeStore stays empty")
                    return@launch
                }
                logger.i("init: loaded ${cachedStatuses.size} cached entries; awaiting user wallets")
                userWalletsListRepository.userWallets.filterNotNull().first()
                val restoredStatuses = restore(cachedStatuses)
                runtimeStore.store(value = restoredStatuses)
                logger.i("init: runtimeStore populated with ${restoredStatuses.size} entries")
            } catch (e: Exception) {
                runSuspendCatching { persistenceDataStore.updateData { emptyMap() } }
                logger.e("Error while loading cached payment account statuses", e)
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<AccountStatus.Payment?> {
        return runtimeStore.get()
            .onStart {
                awaitInitialLoad()
                logger.i("get($userWalletId): subscribed to runtimeStore")
            }
            .onEach { map ->
                logger.i(
                    "get($userWalletId): runtimeStore emitted map size=${map.size}, " +
                        "hasEntry=${map.containsKey(userWalletId.stringValue)}",
                )
            }
            .map { it[userWalletId.stringValue] }
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): AccountStatus.Payment? {
        awaitInitialLoad()
        val result = runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
        logger.i("getSyncOrNull($userWalletId) valueType=${result?.value?.let { it::class.simpleName } ?: "null"}")
        return result
    }

    suspend fun updateStatusSource(userWalletId: UserWalletId, source: StatusSource) {
        awaitInitialLoad()
        runtimeStore.update(emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val paymentAccountStatus = this[userWalletId.stringValue] ?: return@update stored
                val newValue = paymentAccountStatus.copy(value = paymentAccountStatus.value.copySealed(source = source))
                put(key = userWalletId.stringValue, value = newValue)
            }
        }
    }

    /**
     * Optimistically marks the cached VA on-ramp as [VirtualAccountOnramp.Processing] so the UI reflects a

     * read-modify-write atomically inside [RuntimeSharedStore.update] to avoid a lost update racing with a
     * concurrent [store]/[updateStatusSource] call. No-op (no write) when there is no cached entry for
     * [userWalletId], or when its value isn't [PaymentAccountStatusValue.Loaded]. Not persisted, mirroring
     * [updateStatusSource].
     */
    suspend fun markVirtualAccountProcessing(userWalletId: UserWalletId) {
        logger.i("markVirtualAccountProcessing($userWalletId)")
        updateVirtualAccount(userWalletId) { VirtualAccountOnramp.Processing }
    }

    suspend fun markVirtualAccountEligible(userWalletId: UserWalletId) {
        logger.i("markVirtualAccountEligible($userWalletId)")
        updateVirtualAccount(userWalletId) { current ->
            if (current == VirtualAccountOnramp.Failed) VirtualAccountOnramp.Eligible else current
        }
    }

    private suspend fun updateVirtualAccount(
        userWalletId: UserWalletId,
        transform: (VirtualAccountOnramp?) -> VirtualAccountOnramp?,
    ) {
        awaitInitialLoad()
        runtimeStore.update(emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val paymentAccountStatus = this[userWalletId.stringValue] ?: return@update stored
                val loaded = paymentAccountStatus.value as? PaymentAccountStatusValue.Loaded ?: return@update stored
                val virtualAccount = transform(loaded.virtualAccount)
                val newValue = paymentAccountStatus.copy(value = loaded.copy(virtualAccount = virtualAccount))
                put(key = userWalletId.stringValue, value = newValue)
            }
        }
    }

    /**
     * Optimistically flips the cached card of [productInstanceId] to [TangemPayCardState.Activating] so the

     * the order yet. Only a card that is still awaiting activation is touched, so this can never downgrade an
     * already active card. Same atomicity, no-op and non-persistence semantics as
     * [markVirtualAccountProcessing].
     */
    suspend fun markCardActivating(userWalletId: UserWalletId, productInstanceId: String) {
        logger.i("markCardActivating($userWalletId, $productInstanceId)")
        awaitInitialLoad()
        runtimeStore.update(emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val paymentAccountStatus = this[userWalletId.stringValue] ?: return@update stored
                val loaded = paymentAccountStatus.value as? PaymentAccountStatusValue.Loaded ?: return@update stored
                val cards = loaded.cards.map { card ->
                    if (card.productInstanceId == productInstanceId && card.state == TangemPayCardState.Delivering) {
                        card.copy(state = TangemPayCardState.Activating)
                    } else {
                        card
                    }
                }
                val newValue = paymentAccountStatus.copy(value = loaded.copy(cards = cards))
                put(key = userWalletId.stringValue, value = newValue)
            }
        }
    }

    suspend fun store(userWalletId: UserWalletId, status: AccountStatus.Payment) {
        logger.i("store($userWalletId): valueType=${status.value::class.simpleName}")
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

    suspend fun remove(userWalletId: UserWalletId) {
        logger.i("remove($userWalletId)")
        remove(userWalletIds = listOf(userWalletId))
    }

    suspend fun remove(userWalletIds: List<UserWalletId>) {
        logger.i("remove($userWalletIds)")
        awaitInitialLoad()
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

    private fun restore(cachedStatuses: WalletIdWithPaymentStatusDM): WalletIdWithPaymentStatus {
        return buildMap {
            cachedStatuses.forEach { (rawUserWalletId, statusDM) ->
                val account = Account.Payment(userWalletId = UserWalletId(rawUserWalletId))
                try {
                    val statusValue = converter.convertBack(userWalletId = account.userWalletId, value = statusDM)
                    put(rawUserWalletId, AccountStatus.Payment(account = account, value = statusValue))
                } catch (e: Exception) {
                    logger.e("init: skipped cached payment status of ${account.userWalletId}", e)
                }
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

    private suspend fun awaitInitialLoad() = initialLoadJob.join()
}