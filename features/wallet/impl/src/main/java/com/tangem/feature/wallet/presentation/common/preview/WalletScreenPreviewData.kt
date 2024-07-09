package com.tangem.feature.wallet.presentation.common.preview

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData.topBarConfig
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import kotlinx.collections.immutable.persistentListOf

internal object WalletScreenPreviewData {
    private val tokenItemState = TokenItemState.Content(
        id = "1",
        iconState = CurrencyIconState.Locked,
        titleState = TokenItemState.TitleState.Content(text = "Bitcoin"),
        fiatAmountState = TokenItemState.FiatAmountState.Content(text = "12 368,14 \$"),
        cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "0,35853044 BTC"),
        cryptoPriceState = TokenItemState.CryptoPriceState.Content(
            price = "34 496,75 \$",
            priceChangePercent = "0,43 %",
            type = PriceChangeType.DOWN,
        ),
        onItemClick = {},
        onItemLongClick = {},
    )

    private val contentTokensState = WalletTokensListState.ContentState.Content(
        items = persistentListOf(
            WalletTokensListState.TokensListItemState.NetworkGroupTitle(
                id = 1,
                name = stringReference("Bitcoin"),
            ),
            WalletTokensListState.TokensListItemState.Token(state = tokenItemState),
            WalletTokensListState.TokensListItemState.NetworkGroupTitle(
                id = 2,
                name = stringReference("Ethereum"),
            ),
            WalletTokensListState.TokensListItemState.Token(
                state = tokenItemState.copy(
                    id = "2",
                    titleState = TokenItemState.TitleState.Content(text = "Ethereum"),
                    fiatAmountState = TokenItemState.FiatAmountState.Content(text = "3 340,79 \$"),
                    cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "1,856660295 ETH"),
                    cryptoPriceState = TokenItemState.CryptoPriceState.Content(
                        price = "1 799,41 \$",
                        priceChangePercent = "5,16 %",
                        type = PriceChangeType.UP,
                    ),
                ),
            ),
            WalletTokensListState.TokensListItemState.Token(
                state = TokenItemState.Unreachable(
                    id = "3",
                    iconState = CurrencyIconState.Locked,
                    titleState = TokenItemState.TitleState.Content(text = "Polygon"),
                    onItemClick = {},
                    onItemLongClick = {},
                ),
            ),
            WalletTokensListState.TokensListItemState.Token(
                state = tokenItemState.copy(
                    id = "4",
                    titleState = TokenItemState.TitleState.Content(text = "Shiba Inu"),
                    fiatAmountState = TokenItemState.FiatAmountState.Content(text = "48,64 \$"),
                    cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "6 200 220,00 SHIB"),
                    cryptoPriceState = TokenItemState.CryptoPriceState.Content(
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
            balance = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
            onRenameClick = { _ -> },
            onDeleteClick = {},
        )
    }
    private val multiWalletState by lazy {
        WalletState.MultiCurrency.Content(
            pullToRefreshConfig = WalletPullToRefreshConfig(
                isRefreshing = false,
                onRefresh = {},
            ),
            walletCardState = miltiUnreachableCard,
            warnings = persistentListOf(
                WalletNotification.Warning.SomeNetworksUnreachable,
            ),
            bottomSheetConfig = null,
            tokensListState = contentTokensState,
            manageTokensButtonConfig = ManageTokensButtonConfig(onClick = {}),
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
    )
}
