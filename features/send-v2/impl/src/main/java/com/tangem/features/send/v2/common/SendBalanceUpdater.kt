package com.tangem.features.send.v2.common

import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.txhistory.TxHistoryFeatureToggles
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.DelayedWork
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*

@Suppress("LongParameterList")
internal class SendBalanceUpdater @AssistedInject constructor(
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    private val updateDelayedNetworkStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val getTxHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val getTxHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles,
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    @DelayedWork private val coroutineScope: CoroutineScope,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val cryptoCurrency: CryptoCurrency,
) {
    fun scheduleUpdates() {
        coroutineScope.launch {
            listOf(
                // we should update network to find pending tx after 1 sec
                async {
                    fetchPendingTransactionsUseCase(
                        userWalletId = userWallet.walletId,
                        networks = setOf(cryptoCurrency.network),
                    )
                },
                // we should update tx history and network for new balances
                async {
                    updateTxHistory()
                },
                async {
                    updateNetworkStatuses()
                },
            ).awaitAll()
        }
    }

    private suspend fun updateNetworkStatuses(delay: Long = BALANCE_UPDATE_DELAY) {
        updateDelayedNetworkStatusUseCase(
            userWalletId = userWallet.walletId,
            network = cryptoCurrency.network,
            delayMillis = delay,
            refresh = true,
        )
    }

    private suspend fun updateTxHistory() {
        delay(BALANCE_UPDATE_DELAY)
        val txHistoryItemsCountEither = getTxHistoryItemsCountUseCase(
            userWalletId = userWallet.walletId,
            currency = cryptoCurrency,
        )

        txHistoryItemsCountEither.onRight {
            if (txHistoryFeatureToggles.isFeatureEnabled) {
                txHistoryContentUpdateEmitter.triggerUpdate()
            } else {
                getTxHistoryItemsUseCase(
                    userWalletId = userWallet.walletId,
                    currency = cryptoCurrency,
                    refresh = true,
                )
            }
        }
    }

    private companion object {
        const val BALANCE_UPDATE_DELAY = 11_000L
    }

    @AssistedFactory
    interface Factory {
        fun create(cryptoCurrency: CryptoCurrency, userWallet: UserWallet): SendBalanceUpdater
    }
}