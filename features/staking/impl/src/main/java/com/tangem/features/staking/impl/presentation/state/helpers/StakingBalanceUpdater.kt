package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.domain.staking.FetchStakingYieldBalanceUseCase
import com.tangem.domain.staking.FetchActionsUseCase
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.DelayedWork
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*

@Suppress("LongParameterList")
internal class StakingBalanceUpdater @AssistedInject constructor(
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    private val updateDelayedNetworkStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val stakingYieldBalanceUseCase: FetchStakingYieldBalanceUseCase,
    private val getTxHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val getTxHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val fetchActionsUseCase: FetchActionsUseCase,
    @DelayedWork private val coroutineScope: CoroutineScope,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val yield: Yield,
) {
    fun fullUpdate() {
        coroutineScope.launch {
            listOf(
                // we should update network to find pending tx after 1 sec
                async {
                    fetchPendingTransactionsUseCase(
                        userWalletId = userWallet.walletId,
                        networks = setOf(cryptoCurrencyStatus.currency.network),
                    )
                },
                // we should update tx history and network for new balances
                async {
                    updateStakeBalance()
                },
                async {
                    updateTxHistory()
                },
                async {
                    updateNetworkStatuses()
                },
                async {
                    updateProcessingActions()
                },
            ).awaitAll()
        }
    }

    suspend fun partialUpdate() {
        coroutineScope {
            listOf(
                async {
                    updateStakeBalance()
                },
                async {
                    updateNetworkStatuses(delay = 0)
                },
                async {
                    updateProcessingActions()
                },
            ).awaitAll()
        }
    }

    private suspend fun updateNetworkStatuses(delay: Long = BALANCE_UPDATE_DELAY) {
        updateDelayedNetworkStatusUseCase(
            userWalletId = userWallet.walletId,
            network = cryptoCurrencyStatus.currency.network,
            delayMillis = delay,
            refresh = true,
        )
    }

    private suspend fun updateStakeBalance() {
        stakingYieldBalanceUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
            refresh = true,
        )
    }

    private suspend fun updateTxHistory() {
        delay(BALANCE_UPDATE_DELAY)
        val txHistoryItemsCountEither = getTxHistoryItemsCountUseCase(
            userWalletId = userWallet.walletId,
            currency = cryptoCurrencyStatus.currency,
        )

        txHistoryItemsCountEither.onRight {
            getTxHistoryItemsUseCase(
                userWalletId = userWallet.walletId,
                currency = cryptoCurrencyStatus.currency,
                refresh = true,
            )
        }
    }

    private suspend fun updateProcessingActions() {
        fetchActionsUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
            networkType = yield.token.network,
            stakingActionStatus = StakingActionStatus.PROCESSING,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            userWallet: UserWallet,
            yield: Yield,
        ): StakingBalanceUpdater
    }

    private companion object {
        const val BALANCE_UPDATE_DELAY = 11_000L
    }
}