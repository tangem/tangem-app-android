package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState.*
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.update

/**
 * Converter from loading tx history state to [WalletSingleCurrencyState.Content]
 *
 * @property currentStateProvider            current state provider
 * @property clickIntents                    screen click intents
 *
[REDACTED_AUTHOR]
 */
internal class WalletLoadingTxHistoryConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val clickIntents: WalletClickIntents,
) : Converter<Either<TxHistoryStateError, Int>, WalletState> {

    override fun convert(value: Either<TxHistoryStateError, Int>): WalletState {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convertError(error: TxHistoryStateError): WalletState {
        val state = currentStateProvider()

        return if (state is WalletSingleCurrencyState.Content) {
            state.copy(
                txHistoryState = when (error) {
                    is TxHistoryStateError.EmptyTxHistories -> {
                        Empty(onBuyClick = clickIntents::onBuyClick)
                    }
                    is TxHistoryStateError.DataError -> {
                        Error(onReloadClick = clickIntents::onReloadClick)
                    }
                    is TxHistoryStateError.TxHistoryNotImplemented -> {
                        NotSupported(onExploreClick = clickIntents::onExploreClick)
                    }
                },
            )
        } else {
            state
        }
    }

    private fun convert(value: Int): WalletSingleCurrencyState.Content {
        val state = requireNotNull(currentStateProvider() as? WalletSingleCurrencyState.Content)
        val txHistoryContent = requireNotNull(state.txHistoryState as? Content)

        txHistoryContent.contentItems.update {
            PagingData.from(
                data = listOf(TxHistoryItemState.Title(onExploreClick = clickIntents::onExploreClick)) +
                    MutableList(
                        size = value,
                        init = {
                            TxHistoryItemState.Transaction(
                                state = TransactionState.Loading(it.toString()),
                            )
                        },
                    ),
            )
        }

        return state
    }
}