package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.Flow

/**
 * Converter from loaded tx history to [TxHistoryState]
 *
 * @property currentStateProvider            current state provider
 * @property currentCardTypeResolverProvider current card type resolver provider
 * @property clickIntents                    screen click intents
 *
 * @author Andrew Khokhlov on 28/07/2023
 */
internal class WalletLoadedTxHistoryConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val clickIntents: WalletClickIntents,
) : Converter<Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>, WalletState> {

    private val walletTxHistoryItemFlowConverter by lazy {
        WalletTxHistoryItemFlowConverter(
            currentStateProvider = currentStateProvider,
            blockchain = currentCardTypeResolverProvider().getBlockchain(),
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>): WalletState {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convertError(error: TxHistoryListError): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletSingleCurrencyState.Content -> {
                state.copy(
                    txHistoryState = when (error) {
                        is TxHistoryListError.DataError -> {
                            TxHistoryState.Error(
                                onReloadClick = clickIntents::onReloadClick,
                                onExploreClick = clickIntents::onExploreClick,
                            )
                        }
                    },
                )
            }
            is WalletMultiCurrencyState.Content,
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> state
        }
    }

    private fun convert(items: Flow<PagingData<TxHistoryItem>>): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletSingleCurrencyState.Content -> {
                return state.copy(
                    txHistoryState = walletTxHistoryItemFlowConverter.convert(value = items) ?: state.txHistoryState,
                )
            }
            is WalletMultiCurrencyState.Content,
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> state
        }
    }
}
