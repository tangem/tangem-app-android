package com.tangem.features.onramp.swap.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.rows.NetworkTitle
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.dashedBorder
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.entity.ExchangeCardUM

/**
 * Exchange card
 *
 * @param state           state
 * @param isBalanceHidden is balance hidden
 * @param modifier        modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun ExchangeCard(state: ExchangeCardUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            ),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Title(titleReference = state.titleReference, removeButtonUM = state.removeButtonUM)

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90))
                    .togetherWith(fadeOut(animationSpec = tween(durationMillis = 90)))
            },
            label = "TokenItem's changing",
        ) { animatedState ->
            when (animatedState) {
                is ExchangeCardUM.Empty -> EmptyTokenBlock(text = animatedState.subtitleReference)
                is ExchangeCardUM.Filled -> {
                    TokenItem(state = animatedState.tokenItemState, isBalanceHidden = isBalanceHidden)
                }
            }
        }
    }
}

@Composable
private fun Title(titleReference: TextReference, removeButtonUM: ExchangeCardUM.RemoveButtonUM?) {
    NetworkTitle(
        title = {
            Text(
                text = titleReference.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TangemTheme.typography.subtitle2,
            )
        },
        action = { RemoveButton(state = removeButtonUM) },
    )
}

@Composable
private fun RemoveButton(state: ExchangeCardUM.RemoveButtonUM?) {
    AnimatedVisibility(visible = state != null) {
        state ?: return@AnimatedVisibility

        Text(
            text = stringResource(id = R.string.manage_tokens_remove),
            modifier = Modifier.clickable(
                indication = ripple(bounded = false),
                interactionSource = remember { MutableInteractionSource() },
                onClick = state.onClick,
            ),
            color = TangemTheme.colors.text.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
private fun EmptyTokenBlock(text: TextReference, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 13.dp)
            .heightIn(min = 50.dp)
            .fillMaxWidth()
            .dashedBorder(
                color = TangemTheme.colors.icon.informative,
                shape = RoundedCornerShape(16.dp),
                dashLength = 2.dp,
                gapLength = 6.dp,
            )
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.resolveReference(),
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography.body2,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ExchangeCard(@PreviewParameter(ExchangeCardUMProvider::class) state: ExchangeCardUM) {
    TangemThemePreview {
        ExchangeCard(
            state = state,
            isBalanceHidden = false,
            modifier = Modifier
                .background(TangemTheme.colors.background.secondary)
                .padding(16.dp),
        )
    }
}

private class ExchangeCardUMProvider : PreviewParameterProvider<ExchangeCardUM> {

    override val values: Sequence<ExchangeCardUM> = sequenceOf(
        ExchangeCardUM.Empty(
            titleReference = resourceReference(id = R.string.swapping_from_title),
            subtitleReference = resourceReference(id = R.string.action_buttons_you_want_to_swap),
        ),
        createFilled(removeButtonUM = null),
        createFilled(removeButtonUM = ExchangeCardUM.RemoveButtonUM { }),
    )

    private fun createFilled(removeButtonUM: ExchangeCardUM.RemoveButtonUM?): ExchangeCardUM.Filled {
        return ExchangeCardUM.Filled(
            titleReference = resourceReference(id = R.string.swapping_from_title),
            removeButtonUM = removeButtonUM,
            tokenItemState = TokenItemState.Content(
                id = "1",
                iconState = CurrencyIconState.Locked,
                titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Bitcoin")),
                fiatAmountState = TokenItemState.FiatAmountState.Content(text = "12 368,14 \$"),
                subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "0,35853044 BTC"),
                subtitleState = TokenItemState.SubtitleState.CryptoPriceContent(
                    price = "34 496,75 \$",
                    priceChangePercent = "0,43 %",
                    type = PriceChangeType.DOWN,
                ),
                onItemClick = {},
                onItemLongClick = {},
            ),
        )
    }
}