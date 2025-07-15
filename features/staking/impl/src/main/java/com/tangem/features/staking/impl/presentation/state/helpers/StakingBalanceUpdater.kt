package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.domain.staking.FetchActionsUseCase
import com.tangem.domain.staking.FetchStakingYieldBalanceUseCase
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.DelayedWork
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*

@Suppress("LongParameterList")
internal class StakingBalanceUpdater @AssistedInject constructor(
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    private val getTxHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val fetchActionsUseCase: FetchActionsUseCase,
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val fetchStakingYieldBalanceUseCase: FetchStakingYieldBalanceUseCase,
    @DelayedWork private val coroutineScope: CoroutineScope,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val yield: Yield,
) {
    fun updateAfterTransaction() {
        coroutineScope.launch {
            listOf(
                // we should update network to find pending tx after 1 sec
                async {
                    fetchPendingTransactionsUseCase(
                        userWalletId = userWallet.walletId,
                        network = cryptoCurrencyStatus.currency.network,
                    )
                },
                async {
                    fetchStakingYieldBalanceUseCase(
                        userWalletId = userWallet.walletId,
                        cryptoCurrency = cryptoCurrencyStatus.currency,
                    )
                },
                // we should update tx history and network for new balances
                async {
                    fetchCurrencyStatus(delayMillis = BALANCE_UPDATE_DELAY)
                },
                async {
                    updateTxHistory()
                },
                async {
                    updateStakingActions()
                },
            ).awaitAll()
        }
    }

    suspend fun partialUpdate() {
        coroutineScope {
            listOf(
                async {
                    /*
                     * It is important to use NonCancellable here to ensure the update is not interrupted midway.
                     * For example, this can happen if the user enters and immediately leaves the screen.
                     */
                    withContext(NonCancellable) { fetchCurrencyStatus() }
                },
                async {
                    updateStakingActions()
                },
            ).awaitAll()
        }
    }

    private suspend fun fetchCurrencyStatus(delayMillis: Long = 0L) {
        delay(delayMillis)
        fetchCurrencyStatusUseCase(
            userWalletId = userWallet.walletId,
            id = cryptoCurrencyStatus.currency.id,
        )
    }

    private suspend fun updateTxHistory() {
        delay(BALANCE_UPDATE_DELAY)
        val txHistoryItemsCountEither = getTxHistoryItemsCountUseCase(
            userWalletId = userWallet.walletId,
            currency = cryptoCurrencyStatus.currency,
        )

        txHistoryItemsCountEither.onRight {
            txHistoryContentUpdateEmitter.triggerUpdate()
        }
    }

    private suspend fun updateStakingActions() {
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