package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.Flow

/**
 * Converter from loaded tx history to [WalletTxHistoryState]
 *
 * @property currentStateProvider            current state provider
 * @property currentCardTypeResolverProvider current card type resolver provider
 * @property clickIntents                    screen click intents
 *
* [REDACTED_AUTHOR]
 */
internal class WalletLoadedTxHistoryConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val clickIntents: WalletClickIntents,
) : Converter<Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>, WalletState> {

    private val walletTxHistoryItemFlowConverter by lazy {
        WalletTxHistoryItemFlowConverter(
            blockchain = currentCardTypeResolverProvider().getBlockchain(),
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>): WalletState {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convertError(error: TxHistoryListError): WalletState {
        return requireNotNull(currentStateProvider() as? WalletSingleCurrencyState.Content).copy(
            txHistoryState = when (error) {
                is TxHistoryListError.DataError -> {
                    WalletTxHistoryState.Error(onReloadClick = clickIntents::onReloadClick)
                }
            },
        )
    }

    private fun convert(items: Flow<PagingData<TxHistoryItem>>): WalletState {
        return requireNotNull(currentStateProvider() as? WalletSingleCurrencyState.Content).copy(
            txHistoryState = walletTxHistoryItemFlowConverter.convert(value = items),
        )
    }
}
