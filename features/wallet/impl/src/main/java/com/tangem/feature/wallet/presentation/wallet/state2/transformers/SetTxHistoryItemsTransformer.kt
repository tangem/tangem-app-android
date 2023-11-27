package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import androidx.paging.PagingData
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
            is WalletState.SingleCurrency.Content -> {
                val converter = TxHistoryItemFlowConverter(
                    userWallet = userWallet,
                    currentState = prevState,
                    clickIntents = clickIntents,
                )
                prevState.copy(
                    txHistoryState = converter.convert(value = flow),
                )
            }
            is WalletState.SingleCurrency.Locked,
            -> {
                Timber.e("Impossible to load transactions history for locked wallet")
                prevState
            }
            is WalletState.MultiCurrency -> {
                Timber.e("Impossible to load transactions history for multi-currency wallet")
                prevState
            }
        }
    }
}
