package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.currency.icon.CoinIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewMyPortfolioUMProvider
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.utils.StringsSigns

// TODO add rest of the balance states ([REDACTED_TASK_KEY] [Markets] Portfolio token item UI Improvement)
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PortfolioItem(state: PortfolioTokenUM, lastInList: Boolean, modifier: Modifier = Modifier) {
    val hapticManager = LocalHapticManager.current

    Column(modifier) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        hapticManager.perform(TangemHapticEffect.View.ContextClick)
                        state.onClick()
                    },
                    onLongClick = {
                        hapticManager.perform(TangemHapticEffect.View.LongPress)
                        state.onLongTap()
                    },
                )
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .padding(
                    vertical = TangemTheme.dimens.spacing15,
                    horizontal = TangemTheme.dimens.spacing12,
                ),
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Content(state)
        }

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

@Composable
private fun RowScope.Content(state: PortfolioTokenUM) {
    // TODO add custom token
    CoinIcon(
        modifier = Modifier.size(TangemTheme.dimens.size36),
        url = state.iconUrl,
        alpha = 1f, // TODO add disabled state
        colorFilter = null,
        fallbackResId = R.drawable.ic_custom_token_44,
    )

    Column(
        modifier = Modifier.align(Alignment.CenterVertically),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
    ) {
        when (state.balanceContent) {
            is PortfolioTokenUM.BalanceContent.Disabled -> {
                Disabled(
                    state = state,
                    disabledText = state.balanceContent.text.resolveReference(),
                )
            }
            PortfolioTokenUM.BalanceContent.Loading -> {
                Loading(state = state)
            }
            is PortfolioTokenUM.BalanceContent.TokenBalance -> {
                TokenBalance(
                    state = state,
                    content = state.balanceContent,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.TokenBalance(state: PortfolioTokenUM, content: PortfolioTokenUM.BalanceContent.TokenBalance) {
    val balance = if (content.hidden) {
        StringsSigns.STARS
    } else {
        content.balance
    }
    val tokenAmount = if (content.hidden) {
        StringsSigns.STARS
    } else {
        content.tokenAmount
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = state.title,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            modifier = Modifier.alignByBaseline(),
            text = balance,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = state.subtitle,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
        Text(
            text = tokenAmount,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun ColumnScope.Loading(state: PortfolioTokenUM) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = state.title,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        TextShimmer(
            modifier = Modifier
                .width(TangemTheme.dimens.size40)
                .alignByBaseline(),
            style = TangemTheme.typography.body2,
            textSizeHeight = true,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = state.subtitle,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
        TextShimmer(
            modifier = Modifier
                .width(TangemTheme.dimens.size40)
                .alignByBaseline(),
            style = TangemTheme.typography.caption2,
            textSizeHeight = true,
        )
    }
}

@Composable
private fun Disabled(state: PortfolioTokenUM, disabledText: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
        ) {
            Text(
                text = state.title,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = state.subtitle,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }

        Text(
            text = disabledText,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        var quickActionsShown by remember { mutableStateOf(false) }
        var quickActionsShown2 by remember { mutableStateOf(false) }
        val sampleToken = PreviewMyPortfolioUMProvider().sampleToken

        Box(
            Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary),
        ) {
            Column {
                PortfolioItem(
                    state = sampleToken
                        .copy(
                            onClick = {
                                if (quickActionsShown2) {
                                    quickActionsShown2 = false
                                }
                                quickActionsShown = quickActionsShown.not()
                            },
                            isQuickActionsShown = quickActionsShown,
                        ),
                    lastInList = true,
                )
                PortfolioItem(
                    state = sampleToken
                        .copy(
                            onClick = {
                                if (quickActionsShown) {
                                    quickActionsShown = false
                                }
                                quickActionsShown2 = quickActionsShown2.not()
                            },
                            isQuickActionsShown = quickActionsShown2,
                        ),
                    lastInList = true,
                )
                PortfolioItem(
                    state = sampleToken
                        .copy(
                            balanceContent = (
                                sampleToken.balanceContent
                                    as PortfolioTokenUM.BalanceContent.TokenBalance
                                )
                                .copy(hidden = true),
                        ),
                    lastInList = true,
                )
                PortfolioItem(
                    state = sampleToken
                        .copy(
                            balanceContent = PortfolioTokenUM.BalanceContent.Disabled(
                                stringReference("No Address"),
                            ),
                        ),
                    lastInList = true,
                )
                PortfolioItem(
                    state = sampleToken
                        .copy(balanceContent = PortfolioTokenUM.BalanceContent.Loading),
                    lastInList = true,
                )
            }
        }
    }
}