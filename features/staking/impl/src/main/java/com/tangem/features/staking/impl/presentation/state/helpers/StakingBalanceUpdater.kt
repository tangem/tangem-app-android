package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.FetchActionsUseCase
import com.tangem.domain.staking.FetchStakingYieldBalanceUseCase
import com.tangem.domain.staking.GetActionsUseCase
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull

@Suppress("LongParameterList")
internal class StakingBalanceUpdater @AssistedInject constructor(
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    private val getTxHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val fetchActionsUseCase: FetchActionsUseCase,
    private val getActionsUseCase: GetActionsUseCase,
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    private val fetchStakingYieldBalanceUseCase: FetchStakingYieldBalanceUseCase,
    private val coroutineScope: AppCoroutineScope,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val integration: StakingIntegration,
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
                    refreshStakingActionsUntilSettled()
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

    suspend fun partialUpdateWithDelay() {
        delay(BALANCE_UPDATE_DELAY)
        partialUpdate()
    }

    private suspend fun fetchCurrencyStatus(delayMillis: Long = 0L) {
        delay(delayMillis)
        cryptoCurrencyBalanceFetcher.invokeAndAwait(
            userWalletId = userWallet.walletId,
            currency = cryptoCurrencyStatus.currency,
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
            networkType = integration.token.network,
            stakingActionStatus = StakingActionStatus.PROCESSING,
        )
    }

    /**
     * The "pending" overlay on the staking screen is driven by StakeKit actions with status
     * [StakingActionStatus.PROCESSING]. A single post-submit fetch always captures the just-submitted
     * action as PROCESSING, so without a follow-up the badge would never disappear. Re-fetch the
     * actions until the backend stops reporting any as PROCESSING, bounded by [ACTIONS_POLL_MAX_ATTEMPTS]
     * so we never poll indefinitely if it never settles.
     */
    private suspend fun refreshStakingActionsUntilSettled() {
        updateStakingActions()
        repeat(ACTIONS_POLL_MAX_ATTEMPTS) {
            if (!hasProcessingActions()) return
            delay(ACTIONS_POLL_INTERVAL)
            updateStakingActions()
        }
    }

    private suspend fun hasProcessingActions(): Boolean {
        return getActionsUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyId = cryptoCurrencyStatus.currency.id,
        )
            .firstOrNull()
            ?.getOrNull()
            .orEmpty()
            .any { it.status == StakingActionStatus.PROCESSING }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            userWallet: UserWallet,
            integration: StakingIntegration,
        ): StakingBalanceUpdater
    }

    private companion object {
        const val BALANCE_UPDATE_DELAY = 11_000L
        const val ACTIONS_POLL_INTERVAL = 10_000L
        const val ACTIONS_POLL_MAX_ATTEMPTS = 6
    }
}