package com.tangem.features.markets.portfolio.add.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.icons.badge.drawBadge
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.add.impl.ui.state.TokenActionsUM
import com.tangem.features.markets.portfolio.impl.ui.state.PortfolioTokenUM
import com.tangem.features.markets.portfolio.impl.ui.state.QuickActionUM
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

@Composable
internal fun TokenActionsContent(state: TokenActionsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        TokenItem(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action),
            state = state.token,
            isBalanceHidden = false,
        )

        SpacerH(TangemTheme.dimens.spacing14)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(TangemTheme.colors.background.action),
        ) {
            state.quickActions.actions.fastForEach {
                key(it.title) {
                    ActionRow(
                        state = it,
                        onClick = { state.quickActions.onQuickActionClick(it) },
                        onLongClick = { state.quickActions.onQuickActionLongClick(it) },
                    )
                }
            }
        }

        SpacerH16()

        SecondaryButton(
            modifier = modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_later),
            onClick = state.onLaterClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionRow(
    state: QuickActionUM,
    onClick: () -> Unit,
    onLongClick: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    val hapticManager = LocalHapticManager.current
    val onLongClickInternal = {
        hapticManager.perform(TangemHapticEffect.View.LongPress)
        onLongClick()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongClickInternal.takeIf { state.longClickAvailable },
                onClick = {
                    hapticManager.perform(TangemHapticEffect.View.SegmentTick)
                    onClick()
                },
            )
            .padding(horizontal = TangemTheme.dimens.spacing12, vertical = TangemTheme.dimens.spacing15),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        val containerColor = TangemTheme.colors.background.action
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                )
                .size(36.dp)
                .drawWithContent {
                    drawContent()
                    if (state is QuickActionUM.Exchange && state.showBadge) {
                        drawBadge(containerColor = containerColor, offset = 4.dp)
                    }
                },
        ) {
            Icon(
                modifier = Modifier.requiredSize(TangemTheme.dimens.size16),
                imageVector = ImageVector.vectorResource(id = state.icon),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = state.description.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(TokenActionsContentPreviewProvider::class) state: TokenActionsUM) {
    TangemThemePreview {
        TokenActionsContent(
            state = state,
        )
    }
}

private class TokenActionsContentPreviewProvider : PreviewParameterProvider<TokenActionsUM> {
    private val tokenState
        get() = TokenItemState.Content(
            id = UUID.randomUUID().toString(),
            iconState = CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = R.drawable.img_eth_22,
                fallbackTint = TangemColorPalette.Black,
                fallbackBackground = TangemColorPalette.Meadow,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
            titleState = TokenItemState.TitleState.Content(
                text = stringReference(value = "Tether"),
            ),
            fiatAmountState = null,
            subtitle2State = null,
            subtitleState = TokenItemState.SubtitleState.TextContent(value = stringReference("USDT")),
            onItemClick = {},
            onItemLongClick = {},
        )

    override val values: Sequence<TokenActionsUM>
        get() = sequenceOf(
            TokenActionsUM(
                quickActions = PortfolioTokenUM.QuickActions(
                    actions = persistentListOf(
                        QuickActionUM.Buy,
                        QuickActionUM.Exchange(showBadge = true),
                        QuickActionUM.Receive,
                    ),
                    onQuickActionClick = {},
                    onQuickActionLongClick = {},
                ),
                token = tokenState,
                onLaterClick = {},
            ),
        )
}