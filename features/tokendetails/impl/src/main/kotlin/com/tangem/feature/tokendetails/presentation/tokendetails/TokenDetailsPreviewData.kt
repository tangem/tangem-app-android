package com.tangem.feature.tokendetails.presentation.tokendetails

import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsPullToRefreshConfig
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

internal object TokenDetailsPreviewData {

    val tokenDetailsTopAppBarConfig = TokenDetailsTopAppBarConfig(
        onBackClick = {},
        tokenDetailsAppBarMenuConfig = TokenDetailsAppBarMenuConfig(
            persistentListOf(
                TokenDetailsAppBarMenuConfig.MenuItem(
                    title = TextReference.Res(id = R.string.token_details_hide_token),
                    textColorProvider = { TangemTheme.colors.text.warning },
                    onClick = { },
                ),
            ),
        ),
    )

    val tokenInfoBlockStateWithLongNameInMainCurrency = TokenInfoBlockState(
        name = "Stellar (XLM) with long name test",
        iconState = TokenInfoBlockState.IconState.CoinIcon(
            url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
            fallbackResId = R.drawable.img_stellar_22,
            isGrayscale = false,
        ),
        currency = TokenInfoBlockState.Currency.Native,
    )
    val tokenInfoBlockStateWithLongName = TokenInfoBlockState(
        name = "Tether (USDT) with long name test",
        iconState = TokenInfoBlockState.IconState.TokenIcon(
            url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
            fallbackTint = Color.Cyan,
            fallbackBackground = Color.Blue,
            isGrayscale = false,
        ),
        currency = TokenInfoBlockState.Currency.Token(
            standardName = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            networkName = "Ethereum",
        ),
    )

    val tokenInfoBlockState = TokenInfoBlockState(
        name = "Tether USDT",
        iconState = TokenInfoBlockState.IconState.CustomTokenIcon(
            tint = Color.Green,
            background = Color.Magenta,
            isGrayscale = true,
        ),
        currency = TokenInfoBlockState.Currency.Token(
            standardName = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            networkName = "Ethereum",
        ),
    )

    private val actionButtons = persistentListOf(
        TokenDetailsActionButton.Buy(enabled = true, onClick = {}),
        TokenDetailsActionButton.Send(enabled = true, onClick = {}),
        TokenDetailsActionButton.Receive(onClick = {}),
        TokenDetailsActionButton.Swap(enabled = true, onClick = {}),
    )

    val balanceLoading = TokenDetailsBalanceBlockState.Loading(actionButtons = actionButtons)
    val balanceContent = TokenDetailsBalanceBlockState.Content(
        actionButtons = actionButtons,
        fiatBalance = "123,00$",
        cryptoBalance = "866,96 USDT",
    )
    val balanceError = TokenDetailsBalanceBlockState.Error(actionButtons = actionButtons)

    private val marketPriceLoading = MarketPriceBlockState.Loading(currencySymbol = "USDT")

    private val pullToRefreshConfig = TokenDetailsPullToRefreshConfig(
        isRefreshing = false,
        onRefresh = {},
    )

    val tokenDetailsState = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState,
        tokenBalanceBlockState = balanceLoading,
        marketPriceBlockState = marketPriceLoading,
        notifications = persistentListOf(),
        txHistoryState = TxHistoryState.Content(
            contentItems = MutableStateFlow(
                value = TxHistoryState.getDefaultLoadingTransactions {},
            ),
        ),
        dialogConfig = null,
        pendingTxs = persistentListOf(),
        swapTxs = persistentListOf(),
        pullToRefreshConfig = pullToRefreshConfig,
        bottomSheetConfig = null,
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
        event = consumedEvent(),
    )
}
