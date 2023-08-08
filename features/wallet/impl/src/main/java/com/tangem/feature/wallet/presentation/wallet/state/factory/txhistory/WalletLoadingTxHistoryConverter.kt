package com.tangem.feature.wallet.presentation.wallet.state.factory.txhistory

import androidx.paging.PagingData
import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.txhistory.error.TxHistoryStateError
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf

/**
 * Converter from loading tx history to [WalletTxHistoryState]
 *
 * @property currentStateProvider            current state provider
 * @property currentCardTypeResolverProvider current card type resolver provider
 * @property clickIntents                    screen click intents
 *
* [REDACTED_AUTHOR]
 */
internal class WalletLoadingTxHistoryConverter(
    private val currentStateProvider: Provider<WalletStateHolder>,
    private val currentCardTypeResolverProvider: Provider<CardTypesResolver>,
    private val clickIntents: WalletClickIntents,
) : Converter<Either<TxHistoryStateError, Int>, WalletStateHolder> {

    override fun convert(value: Either<TxHistoryStateError, Int>): WalletStateHolder {
        return value.fold(ifLeft = ::convertError, ifRight = ::convert)
    }

    private fun convert(value: Int): WalletStateHolder {
        return currentStateProvider().copySingleCurrencyContent(
            txHistoryState = WalletTxHistoryState.Content(
                items = flowOf(
                    value = PagingData.from(
                        data = buildList(capacity = value) {
                            add(WalletTxHistoryState.TxHistoryItemState.Transaction(state = TransactionState.Loading))
                        },
                    ),
                ),
            ),
        )
    }

    private fun convertError(error: TxHistoryStateError): WalletStateHolder {
        return currentStateProvider().copySingleCurrencyContent(
            txHistoryState = when (error) {
                is TxHistoryStateError.EmptyTxHistories -> {
                    WalletTxHistoryState.Empty(onBuyClick = clickIntents::onBuyClick)
                }
                is TxHistoryStateError.DataError -> {
                    WalletTxHistoryState.Error(onReloadClick = clickIntents::onReloadClick)
                }
                is TxHistoryStateError.TxHistoryNotImplemented -> {
                    WalletTxHistoryState.NotSupported(onExploreClick = clickIntents::onExploreClick)
                }
            },
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
    private fun getButtons(): ImmutableList<ActionButtonConfig> {
        return persistentListOf(
            WalletManageButton.Buy(onClick = {}),
            WalletManageButton.Send(onClick = {}),
            WalletManageButton.Receive(onClick = {}),
            WalletManageButton.Exchange(onClick = {}),
            WalletManageButton.CopyAddress(onClick = {}),
        )
            .map(WalletManageButton::config)
            .toImmutableList()
    }

    private fun getLoadingMarketPriceBlockState(): MarketPriceBlockState {
        return MarketPriceBlockState.Loading(currencyName = currentCardTypeResolverProvider().getBlockchain().currency)
    }
}
