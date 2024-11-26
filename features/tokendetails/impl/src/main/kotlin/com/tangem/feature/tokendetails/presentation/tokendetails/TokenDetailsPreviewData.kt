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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import java.math.BigDecimal

@Suppress("LargeClass")
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

    private val balanceSegmentedButtonConfig = persistentListOf(
        TokenBalanceSegmentedButtonConfig(
            title = resourceReference(R.string.common_all),
            type = BalanceType.ALL,
        ),
        TokenBalanceSegmentedButtonConfig(
            title = resourceReference(R.string.staking_details_available),
            type = BalanceType.AVAILABLE,
        ),
    )

    val balanceLoading = TokenDetailsBalanceBlockState.Loading(
        actionButtons = actionButtons,
        balanceSegmentedButtonConfig = balanceSegmentedButtonConfig,
        selectedBalanceType = BalanceType.ALL,
    )
    val balanceContent = TokenDetailsBalanceBlockState.Content(
        actionButtons = actionButtons,
        fiatBalance = BigDecimal.ZERO,
        cryptoBalance = BigDecimal.ZERO,
        balanceSegmentedButtonConfig = balanceSegmentedButtonConfig,
        selectedBalanceType = BalanceType.ALL,
        onBalanceSelect = {},
        displayCryptoBalance = "966,96 XLM",
        displayFiatBalance = "91,50$",
        isBalanceSelectorEnabled = true,
    )
    val balanceError = TokenDetailsBalanceBlockState.Error(
        actionButtons = actionButtons,
        balanceSegmentedButtonConfig = balanceSegmentedButtonConfig,
        selectedBalanceType = BalanceType.ALL,
    )

    private val marketPriceLoading = MarketPriceBlockState.Loading(currencySymbol = "USDT")

    val stakingTemporaryUnavailableBlock = StakingBlockUM.TemporaryUnavailable
    val stakingLoadingBlock = StakingBlockUM.Loading(iconState)

    val stakingAvailableBlock = StakingBlockUM.StakeAvailable(
        titleText = resourceReference(
            id = R.string.token_details_staking_block_title,
            formatArgs = wrappedList("3.27%"),
        ),
        subtitleText = resourceReference(
            id = R.string.staking_notification_earn_rewards_text_period_day,
            formatArgs = wrappedList("Solana"),
        ),
        iconState = iconState,
        onStakeClicked = {},
    )

    val stakingBalanceBlock = StakingBlockUM.Staked(
        cryptoValue = stringReference("5 SOL"),
        fiatValue = stringReference("456.34 $"),
        rewardValue = resourceReference(R.string.staking_details_no_rewards_to_claim, wrappedList("0.43 $")),
        cryptoAmount = BigDecimal.ZERO,
        fiatAmount = BigDecimal.ZERO,
        onStakeClicked = {},
    )

    private val pullToRefreshConfig = PullToRefreshConfig(
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
        stakingBlocksState = stakingLoadingBlock,
        notifications = persistentListOf(),
        txHistoryState = TxHistoryState.Content(
            contentItems = MutableStateFlow(
                value = TxHistoryState.getDefaultLoadingTransactions {},
            ),
        ),
        dialogConfig = null,
        pendingTxs = persistentListOf(),
        expressTxs = persistentListOf(),
        pullToRefreshConfig = pullToRefreshConfig,
        bottomSheetConfig = null,
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
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
        stakingBlocksState = stakingAvailableBlock,
        notifications = persistentListOf(),
        txHistoryState = TxHistoryState.NotSupported(
            onExploreClick = {},
            pendingTransactions = persistentListOf(),
        ),
        dialogConfig = null,
        pendingTxs = persistentListOf(),
        expressTxs = persistentListOf(),
        pullToRefreshConfig = pullToRefreshConfig,
        bottomSheetConfig = null,
        isBalanceHidden = false,
        isMarketPriceAvailable = true,
        event = consumedEvent(),
    )

    val tokenDetailsState_3 = tokenDetailsState_2.copy(
        txHistoryState = TxHistoryState.Content(
            contentItems = MutableStateFlow(
                value = PagingData.from(txHistoryItems),
            ),
        ),
        stakingBlocksState = stakingBalanceBlock,
    )
}