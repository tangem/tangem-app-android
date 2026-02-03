package com.tangem.features.feed.components.market.details.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.icons.IconTint
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.MarketTokenDetailsBottomSheetTestTags
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.preview.PreviewMyPortfolioUMProvider
import com.tangem.features.feed.components.market.details.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.feed.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState as TokenFiatAmountState

@Composable
internal fun PortfolioItem(state: PortfolioTokenUM, lastInList: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        val hapticManager = LocalHapticManager.current
        val tokenItemState = remember(state.tokenItemState) {
            when (state.tokenItemState) {
                is TokenItemState.Content -> state.tokenItemState.copy(
                    onItemClick = { tokenItemState ->
                        val onClick = state.tokenItemState.onItemClick
                        if (onClick != null) {
                            hapticManager.perform(TangemHapticEffect.View.ContextClick)
                            onClick.invoke(tokenItemState)
                        }
                    },
                )
                else -> state.tokenItemState
            }
        }
        TokenItem(
            state = tokenItemState,
            isBalanceHidden = state.isBalanceHidden,
            itemPaddingValues = PaddingValues(
                start = TangemTheme.dimens.spacing10,
                end = TangemTheme.dimens.spacing12,
            ),
            modifier = Modifier.testTag(MarketTokenDetailsBottomSheetTestTags.PORTFOLIO_TOKEN_ITEM),
        )

        PortfolioQuickActions(
            modifier = Modifier
                .padding(
                    bottom = if (lastInList) {
                        TangemTheme.dimens.spacing12
                    } else {
                        TangemTheme.dimens.spacing24
                    },
                ),
            actions = state.quickActions.actions,
            isVisible = state.isQuickActionsShown,
            onActionClick = state.quickActions.onQuickActionClick,
            onActionLongClick = state.quickActions.onQuickActionLongClick,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PortfolioTokenUMProvider::class) tokenUM: PortfolioTokenUM) {
    TangemThemePreview {
        var isQuickActionsShown by remember { mutableStateOf(value = false) }

        val onItemClick = {
            isQuickActionsShown = isQuickActionsShown.not()
        }

        PortfolioItem(
            modifier = Modifier.background(color = TangemTheme.colors.background.action),
            state = tokenUM.copy(
                tokenItemState = when (tokenUM.tokenItemState) {
                    is TokenItemState.Content -> tokenUM.tokenItemState.copy(onItemClick = { onItemClick() })
                    is TokenItemState.Unreachable -> tokenUM.tokenItemState.copy(onItemClick = { onItemClick() })
                    else -> tokenUM.tokenItemState
                },
                isQuickActionsShown = isQuickActionsShown,
            ),
            lastInList = true,
        )
    }
}

private class PortfolioTokenUMProvider : CollectionPreviewParameterProvider<PortfolioTokenUM>(
    collection = listOf(
        tokenUM.copy(
            tokenItemState = (tokenUM.tokenItemState as TokenItemState.Content).copy(
                fiatAmountState = contentFiatAmount?.copy(
                    icons = persistentListOf(
                        TokenFiatAmountState.Content.IconUM(
                            iconRes = R.drawable.ic_staking_24,
                            tint = IconTint.Accent,
                        ),
                    ),
                ),
            ),
        ),
        tokenUM.copy(
            tokenItemState = tokenUM.tokenItemState.copy(
                fiatAmountState = contentFiatAmount?.copy(text = DASH_SIGN),
                subtitle2State = (tokenUM.tokenItemState.subtitle2State as? TokenItemState.Subtitle2State.TextContent)
                    ?.copy(text = DASH_SIGN),
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
        val contentFiatAmount = tokenUM.tokenItemState.fiatAmountState as? TokenFiatAmountState.Content
    }
}