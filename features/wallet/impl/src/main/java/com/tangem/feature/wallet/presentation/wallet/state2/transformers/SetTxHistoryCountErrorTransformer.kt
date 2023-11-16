package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.TxHistoryItemStateConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * @author Andrew Khokhlov on 22/11/2023
 */
internal class SetTxHistoryCountErrorTransformer(
    private val userWallet: UserWallet,
    private val error: TxHistoryStateError,
    private val pendingTransactions: Set<TxHistoryItem>,
    private val clickIntents: WalletClickIntentsV2,
) : WalletStateTransformer(userWallet.walletId) {

    private val txHistoryItemConverter by lazy {
        val blockchain = userWallet.scanResponse.cardTypesResolver.getBlockchain()
        TxHistoryItemStateConverter(
            symbol = blockchain.currency,
            decimals = blockchain.decimals(),
            clickIntents = clickIntents,
        )
    }

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> prevState.toErrorState()
            is WalletState.MultiCurrency,
            -> {
                Timber.e("Impossible to load transactions history for multi-currency wallet")
                prevState
            }
            is WalletState.SingleCurrency.Locked,
            -> {
                Timber.e("Impossible to load transactions history for locked wallet")
                prevState
            }
        }
    }

    private fun WalletState.SingleCurrency.Content.toErrorState(): WalletState {
        return copy(
            txHistoryState = when (error) {
                is TxHistoryStateError.EmptyTxHistories -> {
                    TxHistoryState.Empty(onExploreClick = clickIntents::onExploreClick)
                }
                is TxHistoryStateError.DataError -> {
                    TxHistoryState.Error(
                        onReloadClick = clickIntents::onReloadClick,
                        onExploreClick = clickIntents::onExploreClick,
                    )
                }
                is TxHistoryStateError.TxHistoryNotImplemented -> {
                    TxHistoryState.NotSupported(
                        pendingTransactions = txHistoryItemConverter.convertList(pendingTransactions)
                            .toImmutableList(),
                        onExploreClick = clickIntents::onExploreClick,
                    )
                }
            },
        )
    }
}
