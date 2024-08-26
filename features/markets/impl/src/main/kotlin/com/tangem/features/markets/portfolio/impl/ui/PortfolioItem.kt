package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewMyPortfolioUMProvider
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState as TokenFiatAmountState

@Composable
internal fun PortfolioItem(state: PortfolioTokenUM, lastInList: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        TokenItem(state = state.tokenItemState, isBalanceHidden = state.isBalanceHidden)

        PortfolioQuickActions(
            modifier = Modifier.padding(
                bottom = if (lastInList) {
                    TangemTheme.dimens.spacing12
                } else {
                    TangemTheme.dimens.spacing24
                },
            ),
            isVisible = state.isQuickActionsShown,
            onActionClick = state.onQuickActionClick,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PortfolioTokenUMProvider::class) tokenUM: PortfolioTokenUM) {
    TangemThemePreview {
        var quickActionsShown by remember { mutableStateOf(value = false) }

        val onItemClick = {
            quickActionsShown = quickActionsShown.not()
        }

        PortfolioItem(
            state = tokenUM.copy(
                tokenItemState = when (tokenUM.tokenItemState) {
                    is TokenItemState.Content -> tokenUM.tokenItemState.copy(onItemClick = onItemClick)
                    is TokenItemState.Unreachable -> tokenUM.tokenItemState.copy(onItemClick = onItemClick)
                    else -> tokenUM.tokenItemState
                },
                isQuickActionsShown = quickActionsShown,
            ),
            lastInList = true,
        )
    }
}

private class PortfolioTokenUMProvider : CollectionPreviewParameterProvider<PortfolioTokenUM>(
    collection = listOf(
        tokenUM.copy(
            tokenItemState = (tokenUM.tokenItemState as TokenItemState.Content).copy(
                fiatAmountState = contentFiatAmount.copy(hasStaked = true),
            ),
        ),
        tokenUM.copy(
            tokenItemState = tokenUM.tokenItemState.copy(
                fiatAmountState = contentFiatAmount.copy(text = DASH_SIGN),
                cryptoAmountState = tokenUM.tokenItemState.cryptoAmountState.copy(text = DASH_SIGN),
            ),
        ),
        tokenUM.copy(isBalanceHidden = true),
        tokenUM.copy(
            tokenItemState = TokenItemState.Unreachable(
                id = tokenUM.tokenItemState.id,
                iconState = tokenUM.tokenItemState.iconState,
                titleState = tokenUM.tokenItemState.titleState,
                subtitleState = tokenUM.tokenItemState.subtitleState,
                onItemClick = {},
                onItemLongClick = {},
            ),
        ),
        tokenUM.copy(
            tokenItemState = TokenItemState.NoAddress(
                id = tokenUM.tokenItemState.id,
                iconState = tokenUM.tokenItemState.iconState,
                titleState = tokenUM.tokenItemState.titleState,
                subtitleState = tokenUM.tokenItemState.subtitleState,
                onItemLongClick = {},
            ),
        ),
        tokenUM.copy(
            tokenItemState = TokenItemState.Loading(
                id = tokenUM.tokenItemState.id,
                iconState = tokenUM.tokenItemState.iconState,
                titleState = tokenUM.tokenItemState.titleState as TokenItemState.TitleState.Content,
                subtitleState = tokenUM.tokenItemState.subtitleState,
            ),
        ),
    ),
) {

    companion object {
        val tokenUM = PreviewMyPortfolioUMProvider().sampleToken
        val contentFiatAmount = tokenUM.tokenItemState.fiatAmountState as TokenFiatAmountState.Content
    }
}
