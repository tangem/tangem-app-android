package com.tangem.domain.polymarket.flow

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.domain.swap.SwapTransactionRepository
import kotlinx.coroutines.coroutineScope
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

    override suspend fun watch(userWalletId: UserWalletId): Unit = coroutineScope {
        launch { openOnFundingSwap(userWalletId = userWalletId) }
        launch { closeWhenSettled(userWalletId = userWalletId) }
    }

    private suspend fun openOnFundingSwap(userWalletId: UserWalletId) {
        val userWallet = userWalletsListRepository.getSyncOrNull(id = userWalletId) ?: return
        val collateralId = collateralCurrencyFactory.createDepositTarget().id

        swapTransactionRepository.getTransactions(userWallet = userWallet, cryptoCurrencyId = collateralId)
            .collect { swapLists ->
                val armed = states.value[userWalletId] as? State.Armed ?: return@collect
                val isFunded = swapLists.orEmpty()
                    .flatMap { it.transactions }
                    .any { it.timestamp >= armed.armedAt }

                if (isFunded) open(userWalletId = userWalletId)
            }
    }

    private suspend fun open(userWalletId: UserWalletId) {
        val status = predictionAccountStatusSupplier(userWalletId = userWalletId).first()
        val balanceAtOpen = (status as? PredictionAccountStatusValue.Active)?.balance

        states.update { it + (userWalletId to State.Open(balanceAtOpen = balanceAtOpen)) }
    }

    private suspend fun closeWhenSettled(userWalletId: UserWalletId) {
        states.map { it[userWalletId] as? State.Open }
            .distinctUntilChanged()
            .collectLatest { open ->
                if (open == null) return@collectLatest

                withTimeoutOrNull(config.fundingWindow) {
                    predictionAccountStatusSupplier(userWalletId = userWalletId)
                        .filterIsInstance<PredictionAccountStatusValue.Active>()
                        .first { open.balanceAtOpen != null && it.balance.compareTo(open.balanceAtOpen) != 0 }
                }

                states.update { it - userWalletId }
            }
    }

    private sealed interface State {
        data class Armed(val armedAt: Long) : State
        data class Open(val balanceAtOpen: BigDecimal?) : State
    }
}