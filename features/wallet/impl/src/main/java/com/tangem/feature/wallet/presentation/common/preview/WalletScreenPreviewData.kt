package com.tangem.feature.wallet.presentation.common.preview

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData.topBarConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.persistentListOf

internal object WalletScreenPreviewData {
    private val tokenItemState = TokenItemState.Content(
        id = "1",
        iconState = CurrencyIconState.Locked,
        titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Bitcoin")),
        fiatAmountState = TokenItemState.FiatAmountState.Content(text = "12 368,14 \$"),
        subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "0,35853044 BTC"),
        subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
            price = "34 496,75 \$",
            priceChangePercent = "0,43 %",
            type = PriceChangeType.DOWN,
        ),
        onItemClick = {},
        onItemLongClick = {},
    )

    private val textContentTokensState = WalletTokensListState.ContentState.Content(
        items = persistentListOf(
            TokensListItemUM.GroupTitle(id = 1, text = stringReference("Network Bitcoin")),
            TokensListItemUM.Token(state = tokenItemState),
            TokensListItemUM.GroupTitle(id = 2, text = stringReference("Network Ethereum")),
            TokensListItemUM.Token(
                state = tokenItemState.copy(
                    id = "2",
                    titleState = TokenItemState.TitleState.Content(text = stringReference("Ethereum")),
                    fiatAmountState = TokenItemState.FiatAmountState.Content(text = "3 340,79 \$"),
                    subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "1,856660295 ETH"),
                    subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                        price = "1 799,41 \$",
                        priceChangePercent = "5,16 %",
                        type = PriceChangeType.UP,
                    ),
                ),
            ),
            TokensListItemUM.Token(
                state = TokenItemState.Unreachable(
                    id = "3",
                    iconState = CurrencyIconState.Locked,
                    titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
                    onItemClick = {},
                    onItemLongClick = {},
                ),
            ),
            TokensListItemUM.Token(
                state = tokenItemState.copy(
                    id = "4",
                    titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Shiba Inu")),
                    fiatAmountState = TokenItemState.FiatAmountState.Content(text = "48,64 \$"),
                    subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "6 200 220,00 SHIB"),
                    subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                        price = "0.01 \$",
                        priceChangePercent = "1,34 %",
                        type = PriceChangeType.DOWN,
                    ),
                ),
            ),
        ),
        organizeTokensButtonConfig = WalletTokensListState.OrganizeTokensButtonConfig(
            isEnabled = true,
            onClick = {},
        ),
    )

    private val noteLockedCard by lazy {
        WalletCardState.LockedContent(
            id = UserWalletId(stringValue = "1"),
            title = "Note",
            additionalInfo = WalletAdditionalInfo(
                hideable = false,
                content = TextReference.Str("Locked"),
            ),
            imageResId = R.drawable.ill_note_btc_120_106,
            onRenameClick = { _ -> },
            onDeleteClick = {},
        )
    }
    private val miltiUnreachableCard by lazy {
        WalletCardState.Content(
            id = UserWalletId(stringValue = "2"),
            title = "Wallet 1",
            additionalInfo = WalletAdditionalInfo(
                hideable = false,
                content = TextReference.Str("Seed phrase"),
            ),
            imageResId = R.drawable.ill_wallet2_cards3_120_106,
            cardCount = 3,
            balance = DASH_SIGN,
            onRenameClick = { _ -> },
            onDeleteClick = {},
            isZeroBalance = false,
        )
    }
    private val multiWalletState by lazy {
        WalletState.MultiCurrency.Content(
            pullToRefreshConfig = PullToRefreshConfig(
                isRefreshing = false,
                onRefresh = {},
            ),
            walletCardState = miltiUnreachableCard,
            buttons = persistentListOf(buyButton),
            warnings = persistentListOf(
                WalletNotification.Warning.SomeNetworksUnreachable,
            ),
            bottomSheetConfig = null,
            tokensListState = textContentTokensState,
        )
    }

    private val buyButton = WalletManageButton.Buy(enabled = false, dimContent = true, onClick = {})
    private val sendButton = WalletManageButton.Send(enabled = false, dimContent = true, onClick = {})
    private val receiveButton = WalletManageButton.Receive(
        enabled = false,
        dimContent = true,
        onClick = {},
        onLongClick = null,
    )

    private val singleWalletLockedState = WalletState.SingleCurrency.Locked(
        walletCardState = noteLockedCard,
        buttons = persistentListOf(
            buyButton,
            sendButton,
            receiveButton,
        ),
        bottomSheetConfig = null,
        onUnlockNotificationClick = {},
        onExploreClick = {},
    )

    internal val walletScreenState = WalletScreenState(
        onBackClick = {},
        topBarConfig = topBarConfig,
        selectedWalletIndex = 0,
        wallets = persistentListOf(
            singleWalletLockedState,
            multiWalletState,
        ),
        onWalletChange = {},
        event = consumedEvent(),
        isHidingMode = false,
        showMarketsOnboarding = false,
        onDismissMarketsOnboarding = {},
    )
}