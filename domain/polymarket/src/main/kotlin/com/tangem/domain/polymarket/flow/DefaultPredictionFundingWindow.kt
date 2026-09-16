package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

class DefaultPredictionFundingWindow(
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val config: PredictionBalancePollingConfig,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : PredictionFundingWindow {

    private val states = MutableStateFlow<Map<UserWalletId, Open>>(emptyMap())

    override fun open(userWalletId: UserWalletId) {
        states.update { it + (userWalletId to Open(openedAt = currentTimeMillis())) }
    }

    override fun isOpen(userWalletId: UserWalletId): Flow<Boolean> {
        return states.map { it.containsKey(userWalletId) }.distinctUntilChanged()
    }

    override suspend fun watch(userWalletId: UserWalletId) {
        states.map { it[userWalletId] }
            .distinctUntilChanged()
            .collectLatest { open ->
                if (open == null) return@collectLatest

                val remainingMillis = (open.openedAt + config.fundingWindow.inWholeMilliseconds - currentTimeMillis())
                    .coerceAtLeast(0)

                withTimeoutOrNull(remainingMillis) { awaitBalanceChange(userWalletId = userWalletId) }

                states.update { if (it[userWalletId] == open) it - userWalletId else it }
            }
    }

    private suspend fun awaitBalanceChange(userWalletId: UserWalletId) {
        val balances = predictionAccountStatusSupplier(userWalletId = userWalletId)
            .filterIsInstance<PredictionAccountStatusValue.Active>()
            .map { it.balance }

        val balanceAtOpen = balances.first()
        balances.first { it.compareTo(balanceAtOpen) != 0 }
    }

    private data class Open(val openedAt: Long)
}