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

    private val states = MutableStateFlow<Map<UserWalletId, State>>(emptyMap())

    override fun open(userWalletId: UserWalletId) {
        val sent = State.Sent(sentAt = currentTimeMillis(), balanceAtOpen = null)
        states.update { it + (userWalletId to sent) }
        appScope.launch { snapshotBalance(userWalletId = userWalletId, sent = sent) }
    }

    override fun isOpen(userWalletId: UserWalletId): Flow<Boolean> {
        return states.map { it[userWalletId] is State.Counting }.distinctUntilChanged()
    }

    override suspend fun watch(userWalletId: UserWalletId) {
        states.map { it[userWalletId] }
            .distinctUntilChanged()
            .collectLatest { state ->
                when (state) {
                    null -> Unit
                    is State.Sent -> startCountdown(userWalletId = userWalletId, sent = state)
                    is State.Counting -> closeWhenSettled(userWalletId = userWalletId, counting = state)
                }
            }
    }

    private suspend fun snapshotBalance(userWalletId: UserWalletId, sent: State.Sent) {
        val balance = withTimeoutOrNull(BALANCE_SNAPSHOT_TIMEOUT) {
            activeBalances(userWalletId = userWalletId).first()
        } ?: return

        states.update {
            if (it[userWalletId] == sent) it + (userWalletId to sent.copy(balanceAtOpen = balance)) else it
        }
    }

    private fun startCountdown(userWalletId: UserWalletId, sent: State.Sent) {
        val now = currentTimeMillis()
        val deadline = minOf(
            now + config.fundingWindow.inWholeMilliseconds,
            sent.sentAt + config.fundingWindowMaxAge.inWholeMilliseconds,
        )
        val counting = State.Counting(deadline = deadline, balanceAtOpen = sent.balanceAtOpen)
        states.update { current ->
            when {
                current[userWalletId] != sent -> current
                deadline <= now -> current - userWalletId
                else -> current + (userWalletId to counting)
            }
        }
    }

    private suspend fun closeWhenSettled(userWalletId: UserWalletId, counting: State.Counting) {
        val remainingMillis = (counting.deadline - currentTimeMillis())
            .coerceIn(0, config.fundingWindow.inWholeMilliseconds)

        withTimeoutOrNull(remainingMillis) {
            awaitBalanceChange(userWalletId = userWalletId, balanceAtOpen = counting.balanceAtOpen)
        }

        states.update { if (it[userWalletId] == counting) it - userWalletId else it }
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

    private sealed interface State {
        val balanceAtOpen: BigDecimal?

        data class Sent(val sentAt: Long, override val balanceAtOpen: BigDecimal?) : State
        data class Counting(val deadline: Long, override val balanceAtOpen: BigDecimal?) : State
    }

    private companion object {
        val BALANCE_SNAPSHOT_TIMEOUT = 2.seconds
    }
}