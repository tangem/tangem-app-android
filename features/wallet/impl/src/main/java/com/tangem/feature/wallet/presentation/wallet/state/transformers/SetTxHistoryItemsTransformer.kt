package com.tangem.feature.wallet.presentation.wallet.state.transformers

import androidx.paging.PagingData
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.transformers.converter.TxHistoryItemFlowConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

internal class SetTxHistoryItemsTransformer(
    userWallet: UserWallet,
    private val flow: Flow<PagingData<TransactionState>>,
    private val clickIntents: WalletClickIntents,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.SingleCurrency.Content -> prevState.copy(
                txHistoryState = prevState.txHistoryState.toContentState(),
            )
            is WalletState.Visa.Content -> prevState.copy(
                txHistoryState = prevState.txHistoryState.toContentState(),
            )
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

    private fun TxHistoryState.toContentState(): TxHistoryState {
        val converter = TxHistoryItemFlowConverter(
            currentState = this,
            clickIntents = clickIntents,
        )

        return converter.convert(flow)
    }
}