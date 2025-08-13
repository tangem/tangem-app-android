package com.tangem.features.send.v2.common

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.nft.RefreshAllNFTUseCase
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.models.wallet.UserWallet
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
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter,
    private val refreshAllNFTUseCase: RefreshAllNFTUseCase,
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
                        network = cryptoCurrency.network,
                    )
                },
                // we should update tx history and network for new balances
                async {
                    updateTxHistory()
                },
                async {
                    updateNetworkStatuses()
                },
                async {
                    updateNFT()
                },
            ).awaitAll()
        }
    }

    private suspend fun updateNFT() {
        delay(BALANCE_UPDATE_DELAY)
        refreshAllNFTUseCase(
            userWalletId = userWallet.walletId,
        )
    }

    private suspend fun updateNetworkStatuses(delay: Long = BALANCE_UPDATE_DELAY) {
        updateDelayedNetworkStatusUseCase(
            userWalletId = userWallet.walletId,
            network = cryptoCurrency.network,
            delayMillis = delay,
        )
    }

    private suspend fun updateTxHistory() {
        delay(BALANCE_UPDATE_DELAY)
        val txHistoryItemsCountEither = getTxHistoryItemsCountUseCase(
            userWalletId = userWallet.walletId,
            currency = cryptoCurrency,
        )

        txHistoryItemsCountEither.onRight {
            txHistoryContentUpdateEmitter.triggerUpdate()
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