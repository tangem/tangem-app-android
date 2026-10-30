package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.data.polymarket.entity.PolymarketSessionKeyReadinessDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.Instant

internal class DefaultPolymarketSessionKeyTradingStore(
    private val readinessDataStore: DataStore<Map<String, PolymarketSessionKeyReadinessDTO>>,
) : PolymarketSessionKeyTradingStore {

    private val refused = MutableStateFlow<Map<UserWalletId, Set<String>>>(emptyMap())
    private val missingOnExchange = MutableStateFlow<Map<UserWalletId, Set<String>>>(emptyMap())

    override suspend fun isReadyOnExchange(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        validUntil: Instant,
    ): Boolean = runSuspendCatching { readinessDataStore.data.first()[userWalletId.stringValue] }
        .onFailure { TangemLogger.e("Failed to read the session key readiness of $userWalletId", it) }
        .getOrNull()
        .let { it?.address == sessionKeyAddress.normalized() && it.validUntil == validUntil.epochSecond }

    override suspend fun markReadyOnExchange(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        validUntil: Instant,
    ) {
        val address = sessionKeyAddress.normalized()
        refused.update { it.without(userWalletId, address) }
        missingOnExchange.update { it.without(userWalletId, address) }

        val record = PolymarketSessionKeyReadinessDTO(address = address, validUntil = validUntil.epochSecond)
        runSuspendCatching { readinessDataStore.updateData { it + (userWalletId.stringValue to record) } }
            .onFailure { TangemLogger.e("Failed to record the session key readiness of $userWalletId", it) }
    }

    override fun isMissingOnExchange(userWalletId: UserWalletId, sessionKeyAddress: String): Boolean =
        sessionKeyAddress.normalized() in missingOnExchange.value[userWalletId].orEmpty()

    override fun setReconciliation(userWalletId: UserWalletId, reconciliation: PolymarketSessionKeyReconciliation) {
        when (reconciliation) {
            PolymarketSessionKeyReconciliation.Unavailable -> Unit
            is PolymarketSessionKeyReconciliation.Checked -> {
                val missing = reconciliation.missingOnExchange.mapTo(mutableSetOf()) { address -> address.normalized() }
                missingOnExchange.update { stored -> stored + (userWalletId to missing) }
            }
        }
    }

    override fun isRefused(userWalletId: UserWalletId, sessionKeyAddress: String): Boolean =
        sessionKeyAddress.normalized() in refused.value[userWalletId].orEmpty()

    override suspend fun markRefused(userWalletId: UserWalletId, sessionKeyAddress: String) {
        val address = sessionKeyAddress.normalized()
        refused.update { it + (userWalletId to it[userWalletId].orEmpty() + address) }

        runSuspendCatching {
            readinessDataStore.updateData { records ->
                val key = userWalletId.stringValue
                if (records[key]?.address == address) records - key else records
            }
        }.onFailure { TangemLogger.e("Failed to drop the readiness of the refused session key of $userWalletId", it) }
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        refused.update { it - userWalletId }
        missingOnExchange.update { it - userWalletId }
        runSuspendCatching { readinessDataStore.updateData { it - userWalletId.stringValue } }
            .onFailure { TangemLogger.e("Failed to clear the session key readiness of $userWalletId", it) }
    }

    private fun Map<UserWalletId, Set<String>>.without(userWalletId: UserWalletId, address: String) =
        this + (userWalletId to this[userWalletId].orEmpty() - address)

    private fun String.normalized(): String = lowercase()
}