package com.tangem.features.commonfeatures.impl.addtoportfolio.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.markets.action.QuickActionUM
import com.tangem.common.ui.markets.action.QuickActions
import com.tangem.common.ui.tokenaction.TokenActionRow
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.badge.TangemBadge
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.core.ui.res.*
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state.PortfolioBadgeUM
import com.tangem.features.commonfeatures.impl.addtoportfolio.ui.state.TokenActionsUM
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal
import java.util.UUID

@Composable
internal fun TokenActionsContentV2(state: TokenActionsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        TokenHeader(
            addedToken = state.token,
            portfolioBadge = state.portfolioBadge,
            isBalanceHidden = state.isBalancesHidden,
        )

        SpacerH(TangemTheme.dimens2.x2)

        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        ) {
            state.quickActions.actions.fastForEach { actionUM ->
                key(actionUM.title) {
                    TokenActionRow(
                        iconRes = actionUM.icon,
                        title = actionUM.title,
                        description = actionUM.description,
                        onClick = { state.quickActions.onQuickActionClick(actionUM) },
                        onLongClick = { state.quickActions.onQuickActionLongClick(actionUM) }
                            .takeIf { actionUM.isLongClickAvailable },
                    )
                }
            }
        }

        SpacerH(TangemTheme.dimens2.x2)

        CompositionLocalProvider(LocalHazeState provides rememberHazeState()) {
            SecondaryTangemButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = state.onLaterClick,
                text = resourceReference(R.string.common_later),
                size = TangemButtonSize.X12,
                shape = TangemButtonShape.Rounded,
            )
        }
    }
}

@Composable
private fun TokenHeader(
    addedToken: TokenItemState,
    isBalanceHidden: Boolean,
    portfolioBadge: PortfolioBadgeUM,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens2.x8),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CurrencyIcon(
            state = addedToken.iconState,
            iconSize = 70.dp,
            networkBadgeSize = 24.dp,
        )

        SpacerH(TangemTheme.dimens2.x5)

        when (val fiat = addedToken.fiatAmountState) {
            is TokenItemState.FiatAmountState.AnnotatedContent -> {
                Text(
                    text = fiat.text.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
                    style = TangemTheme.typography2.titleRegular44,
                    color = TangemTheme.colors2.text.neutral.primary,
                )
                SpacerH(TangemTheme.dimens2.x2)
            }
            else -> Unit
        }

        when (val cryptoAmount = addedToken.subtitle2State) {
            is TokenItemState.Subtitle2State.TextContent -> {
                Text(
                    text = cryptoAmount.text.orMaskWithStars(isBalanceHidden),
                    style = TangemTheme.typography2.bodyMedium16,
                    color = TangemTheme.colors2.text.neutral.secondary,
                )
                SpacerH(TangemTheme.dimens2.x2)
            }
            else -> Unit
        }

        SpacerH(TangemTheme.dimens2.x7)

        when (portfolioBadge) {
            is PortfolioBadgeUM.None -> Unit
            is PortfolioBadgeUM.Account -> TangemBadge(portfolioBadge.badge)
            is PortfolioBadgeUM.Wallet -> WalletPortfolioRow(
                name = portfolioBadge.name,
                deviceIcon = portfolioBadge.deviceIcon,
            )
        }
    }
}

@Composable
private fun WalletPortfolioRow(name: TextReference, deviceIcon: DeviceIconUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(TangemTheme.dimens2.x6)
            .clip(RoundedCornerShape(TangemTheme.dimens2.x4))
            .background(TangemTheme.colors2.markers.backgroundSolidGray)
            .padding(start = TangemTheme.dimens2.x3, end = TangemTheme.dimens2.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        Text(
            text = name.resolveReference(),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.markers.textGray,
            maxLines = 1,
        )
        TangemDeviceIcon(
            state = deviceIcon,
            modifier = Modifier.size(TangemTheme.dimens2.x4),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(TokenActionsContentPreviewProviderV2::class) state: TokenActionsUM) {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level2)
                    .padding(horizontal = 16.dp),
            ) {
                TokenActionsContentV2(
                    state = state,
                )
            }
        }
    }
}

private class TokenActionsContentPreviewProviderV2 : PreviewParameterProvider<TokenActionsUM> {
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
            fiatAmountState = TokenItemState.FiatAmountState.AnnotatedContent(
                text = BigDecimal.ONE.formatStyled {
                    fiat(
                        fiatCurrencyCode = "USD",
                        fiatCurrencySymbol = "$",
                        spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                    ).price()
                },
            ),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(
                "1.01 USDT",
            ),
            subtitleState = TokenItemState.SubtitleState.TextContent(value = stringReference("USDT")),
            onItemClick = {},
            onItemLongClick = {},
        )

    override val values: Sequence<TokenActionsUM>
        get() = sequenceOf(
            TokenActionsUM(
                quickActions = QuickActions(
                    actions = persistentListOf(
                        QuickActionUM.V2.Buy,
                        QuickActionUM.V2.Exchange(shouldShowBadge = true),
                        QuickActionUM.V2.Receive,
                    ),
                    onQuickActionClick = {},
                    onQuickActionLongClick = {},
                ),
                token = tokenState,
                onLaterClick = {},
                portfolioBadge = PortfolioBadgeUM.Wallet(
                    name = stringReference("Wallet 2"),
                    deviceIcon = DeviceIconUM.Stub(cardsCount = 2),
                ),
            ),
        )
}