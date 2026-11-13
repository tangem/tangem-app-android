package com.tangem.data.polymarket.store

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

internal class DefaultPolymarketSessionKeyTradingStore : PolymarketSessionKeyTradingStore {

    private val readiness = MutableStateFlow<Map<UserWalletId, Readiness>>(emptyMap())
    private val refused = MutableStateFlow<Map<UserWalletId, Set<String>>>(emptyMap())
    private val missingOnExchange = MutableStateFlow<Map<UserWalletId, Set<String>>>(emptyMap())

    override suspend fun isReadyOnExchange(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        validUntil: Instant,
    ): Boolean = readiness.value[userWalletId] == Readiness(sessionKeyAddress.normalized(), validUntil.epochSecond)

    override suspend fun markReadyOnExchange(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        validUntil: Instant,
    ) {
        markAuthorized(userWalletId, sessionKeyAddress)
        recordReadiness(userWalletId, sessionKeyAddress.normalized(), validUntil)
    }

    override fun markAuthorized(userWalletId: UserWalletId, sessionKeyAddress: String) {
        val address = sessionKeyAddress.normalized()
        refused.update { it.without(userWalletId, address) }
        missingOnExchange.update { it.without(userWalletId, address) }
    }

    override suspend fun restoreReadyOnExchange(
        userWalletId: UserWalletId,
        sessionKeyAddress: String,
        validUntil: Instant,
    ) {
        if (isRefused(userWalletId, sessionKeyAddress)) return
        recordReadiness(userWalletId, sessionKeyAddress.normalized(), validUntil)
    }

    private fun recordReadiness(userWalletId: UserWalletId, address: String, validUntil: Instant) {
        readiness.update { it + (userWalletId to Readiness(address, validUntil.epochSecond)) }
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
        dropReadiness(userWalletId, address)
    }

    override suspend fun forgetReadyOnExchange(userWalletId: UserWalletId, sessionKeyAddress: String) {
        dropReadiness(userWalletId, sessionKeyAddress.normalized())
    }

    private fun dropReadiness(userWalletId: UserWalletId, address: String) {
        readiness.update { records ->
            if (records[userWalletId]?.address == address) records - userWalletId else records
        }
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        refused.update { it - userWalletId }
        missingOnExchange.update { it - userWalletId }
        readiness.update { it - userWalletId }
    }

    private fun Map<UserWalletId, Set<String>>.without(userWalletId: UserWalletId, address: String) =
        this + (userWalletId to this[userWalletId].orEmpty() - address)

    private fun String.normalized(): String = lowercase()

    private data class Readiness(val address: String, val validUntilEpochSecond: Long)
}