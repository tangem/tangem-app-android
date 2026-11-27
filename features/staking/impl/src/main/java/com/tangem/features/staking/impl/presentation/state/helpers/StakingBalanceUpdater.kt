package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.getOrElse
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.FetchActionsUseCase
import com.tangem.domain.staking.FetchStakingYieldBalanceUseCase
import com.tangem.domain.staking.GetActionsUseCase
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.P2PEthPoolIntegration
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.domain.staking.single.SingleStakingBalanceProducer
import com.tangem.domain.staking.single.SingleStakingBalanceSupplier
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.extensions.isEqualTo
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
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
    private val singleStakingBalanceSupplier: SingleStakingBalanceSupplier,
    private val stakingIdFactory: StakingIdFactory,
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
                    if (integration is P2PEthPoolIntegration) {
                        refreshP2PEthPoolBalanceUntilChanged()
                    } else {
                        fetchStakingBalance()
                    }
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

    private suspend fun fetchStakingBalance() {
        fetchStakingYieldBalanceUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
        )
    }

    /**
     * P2P reads the StakeWise subgraph, which reports a deposit minutes after it lands on-chain and exposes no
     * readiness signal, so the position stays stale until re-fetched.
     */
    private suspend fun refreshP2PEthPoolBalanceUntilChanged() {
        fetchStakingBalance()

        val stakingId = stakingIdFactory.create(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
        ).getOrElse { error ->
            TangemLogger.i("P2P ETH pool balance is not polled after the transaction: $error")
            return
        }
        val balanceParams = SingleStakingBalanceProducer.Params(
            userWalletId = userWallet.walletId,
            stakingId = stakingId,
        )
        val positionBeforeIndexing = readP2PEthPoolPosition(balanceParams)
            ?: P2PEthPoolPosition(cryptoCurrencyStatus.value.stakingBalanceData)

        val positionChanged = withTimeoutOrNull(P2P_BALANCE_POLL_TIMEOUT) {
            do {
                delay(P2P_BALANCE_POLL_INTERVAL)
                fetchStakingBalance()
                val position = readP2PEthPoolPosition(balanceParams)
            } while (position == null || position.isSameAs(positionBeforeIndexing))
        }

        if (positionChanged == null) {
            TangemLogger.i("P2P ETH pool balance did not change within the polling window after the transaction")
        }
    }

    private suspend fun readP2PEthPoolPosition(params: SingleStakingBalanceProducer.Params): P2PEthPoolPosition? {
        return when (val balance = singleStakingBalanceSupplier.getSyncOrNull(params, P2P_BALANCE_READ_TIMEOUT)) {
            is StakingBalance.Error,
            null,
            -> null
            else -> P2PEthPoolPosition(balance)
        }
    }

    private class P2PEthPoolPosition(balance: StakingBalance?) {
        // P2P compounds rewards into the stake, so they are excluded to keep a reward update from passing as indexed
        private val principal = balance?.totalStaked.orZero() - balance?.totalRewards.orZero()
        // An exit request maturing only moves its amount from unstaking to withdrawable, so the queue is compared whole
        private val exitQueue = balance?.unstakingAmount.orZero() + balance?.withdrawableAmount.orZero()

        fun isSameAs(other: P2PEthPoolPosition): Boolean {
            return principal isEqualTo other.principal && exitQueue isEqualTo other.exitQueue
        }
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
        const val P2P_BALANCE_POLL_INTERVAL = 15_000L
        const val P2P_BALANCE_POLL_TIMEOUT = 300_000L
        const val P2P_BALANCE_READ_TIMEOUT = 5_000L
    }
}