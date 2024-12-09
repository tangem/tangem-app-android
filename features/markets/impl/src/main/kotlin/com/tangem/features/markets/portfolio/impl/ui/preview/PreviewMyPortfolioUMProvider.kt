package com.tangem.features.markets.portfolio.impl.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
            MyPortfolioUM.Loading,
            MyPortfolioUM.Unavailable,
            MyPortfolioUM.UnavailableForWallet,
        )

    val sampleToken = PortfolioTokenUM(
        tokenItemState = TokenItemState.Content(
            id = "",
            iconState = CurrencyIconState.Locked,
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "My wallet")),
            fiatAmountState = TokenItemState.FiatAmountState.Content(text = "486,65 \$"),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "733,71097 MATIC"),
            subtitleState = TokenItemState.SubtitleState.TextContent(
                value = stringReference(value = "XRP Ledger token"),
            ),
            onItemClick = {},
            onItemLongClick = {},
        ),
        isQuickActionsShown = false,
        quickActions = PortfolioTokenUM.QuickActions(
            actions = QuickActionUM.entries.toImmutableList(),
            onQuickActionClick = {},
            onQuickActionLongClick = {},
        ),
        isBalanceHidden = false,
        walletId = UserWalletId(""),
    )
}