package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TxHistoryItemStateConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

internal class SetTxHistoryCountErrorTransformer(
    private val userWallet: UserWallet,
    private val error: TxHistoryStateError,
    private val pendingTransactions: Set<TxHistoryItem>,
    private val clickIntents: WalletClickIntents,
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
            is WalletState.SingleCurrency.Content -> prevState.copy(txHistoryState = createErrorState())
            is WalletState.Visa.Content -> prevState.copy(txHistoryState = createErrorState())
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> {
                Timber.w("Impossible to load transactions history for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                Timber.w("Impossible to load transactions history for multi-currency wallet")
                prevState
            }
        }
    }

    private fun createErrorState(): TxHistoryState = when (error) {
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
    }
}