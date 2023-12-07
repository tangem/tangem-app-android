package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import androidx.paging.PagingData
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter.TxHistoryItemFlowConverter
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

internal class SetTxHistoryItemsTransformer(
    private val userWallet: UserWallet,
    private val flow: Flow<PagingData<TxHistoryItem>>,
    private val clickIntents: WalletClickIntentsV2,
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
            userWallet = userWallet,
            currentState = this,
            clickIntents = clickIntents,
        )

        return converter.convert(flow)
    }
}