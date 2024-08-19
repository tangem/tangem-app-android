package com.tangem.features.markets.portfolio.impl.ui.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
        id = "",
        networkId = "",
        iconUrl = "",
        balanceContent = PortfolioTokenUM.BalanceContent.TokenBalance(
            balance = "486,65 \$",
            tokenAmount = "733,71097 MATIC",
            hidden = false,
        ),
        title = "My wallet",
        subtitle = "XRP Ledger token",
        onClick = {},
        onLongTap = {},
        isQuickActionsShown = false,
        onQuickActionClick = {},
    )
}