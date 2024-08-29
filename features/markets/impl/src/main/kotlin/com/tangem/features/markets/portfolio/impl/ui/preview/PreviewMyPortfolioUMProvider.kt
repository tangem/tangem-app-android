package com.tangem.features.markets.portfolio.impl.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import kotlinx.collections.immutable.persistentListOf

internal class PreviewMyPortfolioUMProvider : PreviewParameterProvider<MyPortfolioUM> {

    override val values: Sequence<MyPortfolioUM>
        get() = sequenceOf(
            MyPortfolioUM.Tokens(
                tokens = persistentListOf(sampleToken, sampleToken),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Available,
                onAddClick = {},
            ),
            MyPortfolioUM.Tokens(
                tokens = persistentListOf(sampleToken, sampleToken.copy(isQuickActionsShown = true)),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Unavailable,
                onAddClick = {},
            ),
            MyPortfolioUM.Tokens(
                tokens = persistentListOf(sampleToken.copy(isQuickActionsShown = true), sampleToken),
                buttonState = MyPortfolioUM.Tokens.AddButtonState.Loading,
                onAddClick = {},
            ),
            MyPortfolioUM.AddFirstToken(
                onAddClick = {},
            ),
            MyPortfolioUM.Loading,
            MyPortfolioUM.Unavailable,
        )

    val sampleToken = PortfolioTokenUM(
        tokenItemState = TokenItemState.Content(
            id = "",
            iconState = CurrencyIconState.Locked,
            titleState = TokenItemState.TitleState.Content(text = "My wallet"),
            fiatAmountState = TokenItemState.FiatAmountState.Content(text = "486,65 \$"),
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "733,71097 MATIC"),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = "XRP Ledger token"),
            onItemClick = {},
            onItemLongClick = {},
        ),
        isQuickActionsShown = false,
        onQuickActionClick = {},
        isBalanceHidden = false,
    )
}