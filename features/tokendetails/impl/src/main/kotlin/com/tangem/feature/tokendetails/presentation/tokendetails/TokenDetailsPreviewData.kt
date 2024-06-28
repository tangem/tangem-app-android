package com.tangem.feature.tokendetails.presentation.tokendetails

import androidx.compose.ui.graphics.Color
import androidx.paging.PagingData
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
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
        iconState = IconState.CoinIcon(
            url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
            fallbackResId = R.drawable.img_stellar_22,
            isGrayscale = false,
        ),
        currency = TokenInfoBlockState.Currency.Native,
    )
    val tokenInfoBlockStateWithLongName = TokenInfoBlockState(
        name = "Tether (USDT) with long name test",
        iconState = IconState.TokenIcon(
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

    val tokenInfoBlockStateWithLongNameNoStandard = TokenInfoBlockState(
        name = "Tether (USDT) with long name test",
        iconState = IconState.TokenIcon(
            url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
            fallbackTint = Color.Cyan,
            fallbackBackground = Color.Blue,
            isGrayscale = false,
        ),
        currency = TokenInfoBlockState.Currency.Token(
            standardName = null,
            networkIcon = R.drawable.img_shibarium_22,
            networkName = "Shibarium",
        ),
    )

    val tokenInfoBlockState = TokenInfoBlockState(
        name = "Tether USDT",
        iconState = IconState.CustomTokenIcon(
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

    val iconState = IconState.TokenIcon(
        url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        fallbackTint = Color.Cyan,
        fallbackBackground = Color.Blue,
        isGrayscale = false,
    )

    private val actionButtons = persistentListOf(
        TokenDetailsActionButton.Buy(dimContent = false, onClick = {}),
        TokenDetailsActionButton.Send(dimContent = false, onClick = {}),
        TokenDetailsActionButton.Receive(onClick = {}, onLongClick = null),
        TokenDetailsActionButton.Swap(dimContent = false, onClick = {}),
    )

    val balanceLoading = TokenDetailsBalanceBlockState.Loading(actionButtons = actionButtons)
    val balanceContent = TokenDetailsBalanceBlockState.Content(
        actionButtons = actionButtons,
        fiatBalance = "91,50$",
        cryptoBalance = "966,96 XLM",
    )
    val balanceError = TokenDetailsBalanceBlockState.Error(actionButtons = actionButtons)

    private val marketPriceLoading = MarketPriceBlockState.Loading(currencySymbol = "USDT")

    private val stakingLoading = StakingBlockState.Loading(iconState = iconState)

    private val pullToRefreshConfig = TokenDetailsPullToRefreshConfig(
        isRefreshing = false,
        onRefresh = {},
    )

    private val txHistoryItems = listOf(
        TxHistoryState.TxHistoryItemState.Title(onExploreClick = {}),
        TxHistoryState.TxHistoryItemState.GroupTitle(
            title = "Today",
            itemKey = "Today",
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "1",
                amount = "-0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.OUTGOING,
                onClick = {},
                iconRes = R.drawable.ic_arrow_up_24,
                title = stringReference(value = "Sending"),
                subtitle = stringReference(value = "to: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "2",
                amount = "+0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.INCOMING,
                onClick = {},
                iconRes = R.drawable.ic_arrow_down_24,
                title = stringReference(value = "Receiving"),
                subtitle = stringReference(value = "from: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "3",
                amount = "+0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.INCOMING,
                onClick = {},
                iconRes = R.drawable.ic_doc_24,
                title = stringReference(value = "Approving"),
                subtitle = stringReference(value = "from: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "4",
                amount = "+0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.INCOMING,
                onClick = {},
                iconRes = R.drawable.ic_exchange_vertical_24,
                title = stringReference(value = "Swapping"),
                subtitle = stringReference(value = "contract: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
        TxHistoryState.TxHistoryItemState.GroupTitle(
            title = "Yesterday",
            itemKey = "Yesterday",
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "5",
                amount = "-0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.OUTGOING,
                onClick = {},
                iconRes = R.drawable.ic_arrow_up_24,
                title = stringReference(value = "Sending"),
                subtitle = stringReference(value = "to: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "6",
                amount = "+0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.INCOMING,
                onClick = {},
                iconRes = R.drawable.ic_arrow_down_24,
                title = stringReference(value = "Receiving"),
                subtitle = stringReference(value = "from: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "7",
                amount = "+0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.INCOMING,
                onClick = {},
                iconRes = R.drawable.ic_doc_24,
                title = stringReference(value = "Approving"),
                subtitle = stringReference(value = "from: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
        TxHistoryState.TxHistoryItemState.Transaction(
            state = TransactionState.Content(
                txHash = "8",
                amount = "+0.500913 XLM",
                time = "8:41",
                status = TransactionState.Content.Status.Confirmed,
                direction = TransactionState.Content.Direction.INCOMING,
                onClick = {},
                iconRes = R.drawable.ic_exchange_vertical_24,
                title = stringReference(value = "Swapping"),
                subtitle = stringReference(value = "contract: 33BddS...ga2B"),
                timestamp = 0,
            ),
        ),
    )

    val tokenDetailsState_1 = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState,
        tokenBalanceBlockState = balanceLoading,
        marketPriceBlockState = marketPriceLoading,
        stakingBlockState = stakingLoading,
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
        isStakingAvailable = false,
        event = consumedEvent(),
    )

    val tokenDetailsState_2 = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState.copy(
            name = "Stellar",
        ),
        tokenBalanceBlockState = balanceContent,
        marketPriceBlockState = MarketPriceBlockState.Content(
            currencySymbol = "XLM",
            price = "0.11$",
            priceChangeConfig = PriceChangeState.Content(
                valueInPercent = "5,16%",
                type = PriceChangeType.UP,
            ),
        ),
        stakingBlockState = StakingBlockState.Content(
            interestRate = "7.38",
            periodInDays = 4,
            tokenSymbol = "XLM",
            iconState = iconState,
        ),
        notifications = persistentListOf(),
        txHistoryState = TxHistoryState.NotSupported(
            onExploreClick = {},
            pendingTransactions = persistentListOf(),
        ),
        dialogConfig = null,
        pendingTxs = persistentListOf(),
        swapTxs = persistentListOf(),
        pullToRefreshConfig = pullToRefreshConfig,
        bottomSheetConfig = null,
        isBalanceHidden = false,
        isMarketPriceAvailable = true,
        isStakingAvailable = true,
        event = consumedEvent(),
    )

    val tokenDetailsState_3 = tokenDetailsState_2.copy(
        txHistoryState = TxHistoryState.Content(
            contentItems = MutableStateFlow(
                value = PagingData.from(txHistoryItems),
            ),
        ),
    )
}
