package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryListError
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
    private val currentStateProvider: Provider<WalletStateHolder>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val clickIntents: WalletClickIntents,
) : Converter<Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>, WalletStateHolder> {

    private val walletTxHistoryItemFlowConverter by lazy {
        WalletTxHistoryItemFlowConverter(
            blockchain = currentCardTypeResolverProvider().getBlockchain(),
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: Either<TxHistoryListError, Flow<PagingData<TxHistoryItem>>>): WalletStateHolder {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convertError(error: TxHistoryListError): WalletStateHolder {
        return currentStateProvider().copySingleCurrencyContent(
            txHistoryState = when (error) {
                is TxHistoryListError.DataError -> {
                    WalletTxHistoryState.Error(onReloadClick = clickIntents::onReloadClick)
                }
            },
        )
    }

    private fun convert(items: Flow<PagingData<TxHistoryItem>>): WalletStateHolder {
        return currentStateProvider().copySingleCurrencyContent(
            txHistoryState = walletTxHistoryItemFlowConverter.convert(value = items),
        )
    }

    private fun WalletStateHolder.copySingleCurrencyContent(
        txHistoryState: WalletTxHistoryState,
    ): WalletSingleCurrencyState {
        return WalletSingleCurrencyState.Content(
            onBackClick = onBackClick,
            topBarConfig = topBarConfig,
            walletsListConfig = walletsListConfig,
            pullToRefreshConfig = pullToRefreshConfig,
            notifications = notifications,
            bottomSheetConfig = bottomSheetConfig,
            buttons = getButtons(),
            marketPriceBlockState = getLoadingMarketPriceBlockState(),
            txHistoryState = txHistoryState,
        )
    }
// [REDACTED_TODO_COMMENT]
    private fun getButtons(): ImmutableList<WalletManageButton> {
        return persistentListOf(
            WalletManageButton.Buy(onClick = {}),
            WalletManageButton.Send(onClick = {}),
            WalletManageButton.Receive(onClick = {}),
            WalletManageButton.Exchange(onClick = {}),
            WalletManageButton.CopyAddress(onClick = {}),
        )
    }

    private fun getLoadingMarketPriceBlockState(): MarketPriceBlockState {
        return MarketPriceBlockState.Loading(currencyName = currentCardTypeResolverProvider().getBlockchain().currency)
    }
}
