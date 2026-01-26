package com.tangem.features.feed.components.market.details.portfolio.impl.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.account.AccountIconPreviewData
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.*
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Suppress("PropertyUsedBeforeDeclaration")
internal class PreviewMyPortfolioUMProvider : PreviewParameterProvider<MyPortfolioUM> {

    override val values: Sequence<MyPortfolioUM>
        get() = sequenceOf(
            MyPortfolioUM.Tokens(
                tokens = persistentListOf(sampleToken, sampleToken),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Available,
                addToPortfolioBSConfig = TangemBottomSheetConfig.Empty,
                tokenReceiveBSConfig = TangemBottomSheetConfig.Empty,
                onAddClick = {},
            ),
            MyPortfolioUM.Tokens(
                tokens = persistentListOf(sampleToken, sampleToken.copy(isQuickActionsShown = true)),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Unavailable,
                addToPortfolioBSConfig = TangemBottomSheetConfig.Empty,
                tokenReceiveBSConfig = TangemBottomSheetConfig.Empty,
                onAddClick = {},
            ),
            MyPortfolioUM.Tokens(
                tokens = persistentListOf(sampleToken.copy(isQuickActionsShown = true), sampleToken),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Loading,
                addToPortfolioBSConfig = TangemBottomSheetConfig.Empty,
                tokenReceiveBSConfig = TangemBottomSheetConfig.Empty,
                onAddClick = {},
            ),
            MyPortfolioUM.AddFirstToken(
                addToPortfolioBSConfig = TangemBottomSheetConfig.Empty,
                onAddClick = {},
            ),
            MyPortfolioUM.Content(
                items = persistentListOf(
                    walletPortfolioHeader,
                    accountToken,
                    accountToken,
                ),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Available,
                onAddClick = {},
            ),
            MyPortfolioUM.Content(
                items = persistentListOf(
                    walletHeader,
                    accountHeader,
                    accountToken,
                    accountToken,
                ),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Available,
                onAddClick = {},
            ),
            MyPortfolioUM.Content(
                items = persistentListOf(
                    walletHeader,
                    accountHeader,
                    accountToken.copy(isQuickActionsShown = true),
                    accountToken,
                ),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Available,
                onAddClick = {},
            ),
            MyPortfolioUM.Loading,
            MyPortfolioUM.Unavailable,
            MyPortfolioUM.UnavailableForWallet,
        )

    val walletHeader
        get() = WalletHeader(
            id = UUID.randomUUID().toString(),
            name = stringReference("Wallet 1"),
        )

    val walletPortfolioHeader
        get() = PortfolioHeader(
            state = AccountTitleUM.Text(title = stringReference("Wallet 1")),
            id = UUID.randomUUID().toString(),
        )

    val accountHeader
        get() = PortfolioHeader(
            state = AccountTitleUM.Account(
                icon = AccountIconPreviewData.randomAccountIcon(),
                name = stringReference("Main Account"),
                prefixText = TextReference.EMPTY,
            ),
            id = UUID.randomUUID().toString(),
        )
    val coinIconState
        get() = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = com.tangem.core.ui.R.drawable.img_polygon_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        )
    val accountToken
        get() = sampleToken.copy(
            tokenItemState = TokenItemState.Content(
                id = UUID.randomUUID().toString(),
                iconState = coinIconState,
                titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
                fiatAmountState = FiatAmountState.Content(text = "321 $"),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "5,412 MATIC"),
                subtitleState = TokenItemState.SubtitleState.TextContent(stringReference(value = "Token")),
                onItemClick = {},
                onItemLongClick = {},
            ),
        )

    val sampleToken
        get() = PortfolioTokenUM(
            tokenItemState = TokenItemState.Content(
                id = UUID.randomUUID().toString(),
                iconState = CurrencyIconState.Locked,
                titleState = TokenItemState.TitleState.Content(text = stringReference(value = "My wallet")),
                fiatAmountState = FiatAmountState.Content(text = "486,65 \$"),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "733,71097 MATIC"),
                subtitleState = TokenItemState.SubtitleState.TextContent(
                    value = stringReference(value = "XRP Ledger token"),
                ),
                onItemClick = {},
                onItemLongClick = {},
            ),
            isQuickActionsShown = false,
            quickActions = PortfolioTokenUM.QuickActions(
                actions = persistentListOf(
                    QuickActionUM.Buy,
                    QuickActionUM.Exchange(shouldShowBadge = true),
                    QuickActionUM.Receive,
                ),
                onQuickActionClick = {},
                onQuickActionLongClick = {},
            ),
            isBalanceHidden = false,
            walletId = UserWalletId(""),
        )
}