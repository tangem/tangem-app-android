package com.tangem.feature.tokendetails.presentation.tokendetails

import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.*
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Suppress("LargeClass")
internal object TokenDetailsPreviewData {

    val tokenDetailsTopAppBarConfig = TokenDetailsTopAppBarConfig(
        onBackClick = {},
        tokenDetailsAppBarMenuConfig = TokenDetailsAppBarMenuConfig(
            persistentListOf(
                TangemDropdownMenuItem(
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

    private val iconState = IconState.TokenIcon(
        url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/stellar.png",
        fallbackTint = Color.Cyan,
        fallbackBackground = Color.Blue,
        isGrayscale = false,
    )

    private val actionButtons = persistentListOf(
        TokenDetailsActionButton.Buy(dimContent = false, onClick = {}),
        TokenDetailsActionButton.Send(dimContent = false, onClick = {}),
        TokenDetailsActionButton.Receive(onClick = {}, onLongClick = null),
        TokenDetailsActionButton.Swap(dimContent = false, onClick = {}, showBadge = true),
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
        isBalanceFlickering = false,
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
        titleText = resourceReference(id = R.string.token_details_staking_block_title),
        subtitleText = resourceReference(
            id = R.string.staking_notification_earn_rewards_text_period_day,
            formatArgs = wrappedList("Solana"),
        ),
        iconState = iconState,
        isEnabled = true,
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

    val tokenDetailsState_1 = TokenDetailsState(
        topAppBarConfig = tokenDetailsTopAppBarConfig,
        tokenInfoBlockState = tokenInfoBlockState,
        tokenBalanceBlockState = balanceLoading,
        marketPriceBlockState = marketPriceLoading,
        stakingBlocksState = stakingLoadingBlock,
        notifications = persistentListOf(),
        dialogConfig = null,
        expressTxs = persistentListOf(),
        expressTxsToDisplay = persistentListOf(),
        pullToRefreshConfig = pullToRefreshConfig,
        bottomSheetConfig = null,
        isBalanceHidden = false,
        isMarketPriceAvailable = false,
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
        dialogConfig = null,
        expressTxs = persistentListOf(),
        expressTxsToDisplay = persistentListOf(),
        pullToRefreshConfig = pullToRefreshConfig,
        bottomSheetConfig = null,
        isBalanceHidden = false,
        isMarketPriceAvailable = true,
    )

    val tokenDetailsState_3 = tokenDetailsState_2.copy(stakingBlocksState = stakingBalanceBlock)
}