package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.domain.staking.FetchStakingYieldBalanceUseCase
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
    @DelayedWork private val coroutineScope: CoroutineScope,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) {
    fun scheduleUpdates() {
        coroutineScope.launch {
            listOf(
                // we should update network to find pending tx after 1 sec
                async {
                    fetchPendingTransactionsUseCase(userWallet.walletId, setOf(cryptoCurrencyStatus.currency.network))
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
            ).awaitAll()
        }
    }

    suspend fun instantUpdate() {
        coroutineScope {
            listOf(
                async {
                    updateStakeBalance()
                },
                async {
                    updateNetworkStatuses(delay = 0)
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

    @AssistedFactory
    interface Factory {
        fun create(cryptoCurrencyStatus: CryptoCurrencyStatus, userWallet: UserWallet): StakingBalanceUpdater
    }

    private companion object {
        const val BALANCE_UPDATE_DELAY = 11_000L
    }
}