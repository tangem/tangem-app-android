package com.tangem.domain.polymarket.flow

import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

class DefaultPredictionFundingWindow(
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val config: PredictionBalancePollingConfig,
    private val appScope: AppCoroutineScope,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : PredictionFundingWindow {

    private val states = MutableStateFlow<Map<UserWalletId, Open>>(emptyMap())

    override fun open(userWalletId: UserWalletId) {
        val open = Open(openedAt = currentTimeMillis(), balanceAtOpen = null)
        states.update { it + (userWalletId to open) }
        appScope.launch { snapshotBalance(userWalletId = userWalletId, open = open) }
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
                    .coerceIn(0, config.fundingWindow.inWholeMilliseconds)

                withTimeoutOrNull(remainingMillis) {
                    awaitBalanceChange(userWalletId = userWalletId, balanceAtOpen = open.balanceAtOpen)
                }

                states.update { if (it[userWalletId] == open) it - userWalletId else it }
            }
    }

    private suspend fun snapshotBalance(userWalletId: UserWalletId, open: Open) {
        val balance = withTimeoutOrNull(BALANCE_SNAPSHOT_TIMEOUT) {
            activeBalances(userWalletId = userWalletId).first()
        } ?: return

        states.update {
            if (it[userWalletId] == open) it + (userWalletId to open.copy(balanceAtOpen = balance)) else it
        }
    }

    private suspend fun awaitBalanceChange(userWalletId: UserWalletId, balanceAtOpen: BigDecimal?) {
        val balances = activeBalances(userWalletId = userWalletId)
        val baseline = balanceAtOpen ?: balances.first()

        balances.first { it.compareTo(baseline) != 0 }
    }

    private fun activeBalances(userWalletId: UserWalletId): Flow<BigDecimal> {
        return predictionAccountStatusSupplier(userWalletId = userWalletId)
            .filterIsInstance<PredictionAccountStatusValue.Active>()
            .map { it.balance }
    }

    private data class Open(val openedAt: Long, val balanceAtOpen: BigDecimal?)

    private companion object {
        val BALANCE_SNAPSHOT_TIMEOUT = 2.seconds
    }
}