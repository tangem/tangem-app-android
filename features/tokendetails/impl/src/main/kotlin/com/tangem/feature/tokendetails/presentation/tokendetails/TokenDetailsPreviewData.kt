package com.tangem.feature.tokendetails.presentation.tokendetails

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsAppBarMenuConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
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
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        currency = TokenInfoBlockState.Currency.Native,
    )
    val tokenInfoBlockStateWithLongName = TokenInfoBlockState(
        name = "Tether (USDT) with long name test",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        currency = TokenInfoBlockState.Currency.Token(
            networkName = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            blockchainName = "Ethereum",
        ),
    )

    val tokenInfoBlockState = TokenInfoBlockState(
        name = "Tether USDT",
        iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
        currency = TokenInfoBlockState.Currency.Token(
            networkName = "ERC20",
            networkIcon = R.drawable.img_eth_22,
            blockchainName = "Ethereum",
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

    private val marketPriceLoading = MarketPriceBlockState.Loading(currencyName = "USDT")

    private val pullToRefreshConfig = TokenDetailsPullToRefreshConfig(
        isRefreshing = false,
        onRefresh = {},
    )

    val tokenDetailsState = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState,
        tokenBalanceBlockState = balanceLoading,
        marketPriceBlockState = marketPriceLoading,
        txHistoryState = TxHistoryState.Content(
            contentItems = MutableStateFlow(
                value = TxHistoryState.getDefaultLoadingTransactions {},
            ),
        ),
        dialogConfig = null,
        pendingTxs = persistentListOf(),
        pullToRefreshConfig = pullToRefreshConfig,
    )
}