package com.tangem.domain.polymarket.flow

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.domain.swap.SwapTransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal

class DefaultPredictionFundingWindow(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val swapTransactionRepository: SwapTransactionRepository,
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
    private val config: PredictionBalancePollingConfig,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : PredictionFundingWindow {

    private val states = MutableStateFlow<Map<UserWalletId, State>>(emptyMap())

    override fun arm(userWalletId: UserWalletId) {
        states.update { it + (userWalletId to State.Armed(armedAt = currentTimeMillis())) }
    }

    override fun isOpen(userWalletId: UserWalletId): Flow<Boolean> {
        return states.map { it[userWalletId] is State.Open }.distinctUntilChanged()
    }

    override suspend fun watch(userWalletId: UserWalletId) {
        coroutineScope {
            launch { openOnFundingSwap(userWalletId = userWalletId) }
            launch { closeWhenSettled(userWalletId = userWalletId) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun openOnFundingSwap(userWalletId: UserWalletId) {
        val collateralId = collateralCurrencyFactory.createDepositTarget().id

        userWalletsListRepository.userWallets
            .flatMapLatest { wallets ->
                val allWallets = wallets.orEmpty()
                if (allWallets.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        allWallets.map {
                            swapTransactionRepository.getTransactions(userWallet = it, cryptoCurrencyId = collateralId)
                        },
                    ) { swapLists -> swapLists.flatMap { it.orEmpty() } }
                }
            }
            .collect { swapLists ->
                val armed = states.value[userWalletId] as? State.Armed ?: return@collect
                val fundingSwapAt = swapLists
                    .filter {
                        it.toUserWalletId == userWalletId.stringValue && it.toCryptoCurrencyId == collateralId.value
                    }
                    .flatMap { it.transactions }
                    .mapNotNull { it.timestamp.takeIf { timestamp -> timestamp >= armed.armedAt } }
                    .maxOrNull() ?: return@collect

                open(userWalletId = userWalletId, openedAt = fundingSwapAt)
            }
    }

    private suspend fun open(userWalletId: UserWalletId, openedAt: Long) {
        if (openedAt + config.fundingWindow.inWholeMilliseconds <= currentTimeMillis()) {
            states.update { it - userWalletId }
            return
        }

        val status = predictionAccountStatusSupplier(userWalletId = userWalletId).first()
        val balanceAtOpen = (status as? PredictionAccountStatusValue.Active)?.balance

        val open = State.Open(openedAt = openedAt, balanceAtOpen = balanceAtOpen)
        states.update { it + (userWalletId to open) }
    }

    private suspend fun closeWhenSettled(userWalletId: UserWalletId) {
        states.map { it[userWalletId] as? State.Open }
            .distinctUntilChanged()
            .collectLatest { open ->
                if (open == null) return@collectLatest

                val remainingMillis = (open.openedAt + config.fundingWindow.inWholeMilliseconds - currentTimeMillis())
                    .coerceIn(0, config.fundingWindow.inWholeMilliseconds)

                withTimeoutOrNull(remainingMillis) {
                    predictionAccountStatusSupplier(userWalletId = userWalletId)
                        .filterIsInstance<PredictionAccountStatusValue.Active>()
                        .first { open.balanceAtOpen != null && it.balance.compareTo(open.balanceAtOpen) != 0 }
                }

                states.update { if (it[userWalletId] == open) it - userWalletId else it }
            }
    }

    private sealed interface State {
        data class Armed(val armedAt: Long) : State
        data class Open(val openedAt: Long, val balanceAtOpen: BigDecimal?) : State
    }
}