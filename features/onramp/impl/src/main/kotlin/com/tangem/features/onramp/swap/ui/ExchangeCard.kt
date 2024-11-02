package com.tangem.features.onramp.swap.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.entity.ExchangeCardUM

/**
 * Exchange card
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun ExchangeCard(state: ExchangeCardUM, modifier: Modifier = Modifier) {
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
        NetworkTitle(
            title = {
                Text(
                    text = state.titleReference.resolveReference(),
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TangemTheme.typography.subtitle2,
                )
            },
            action = {
                RemoveButton(hasRemoveButton = state.hasRemoveButton, onClick = {})
            },
        )

        TokenItem(state = state.tokenItemState, isBalanceHidden = false)
    }
}

@Composable
private fun RemoveButton(hasRemoveButton: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible = hasRemoveButton) {
        Text(
            text = stringResource(id = R.string.manage_tokens_remove),
            modifier = Modifier.clickable(
                indication = ripple(bounded = false),
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
            color = TangemTheme.colors.text.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography.body2,
        )
    }
}

@Preview
@Composable
private fun Preview_ExchangeCard(@PreviewParameter(ExchangeCardUMProvider::class) state: ExchangeCardUM) {
    TangemThemePreview {
        ExchangeCard(state = state)
    }
}

private class ExchangeCardUMProvider : PreviewParameterProvider<ExchangeCardUM> {

    override val values: Sequence<ExchangeCardUM> = sequenceOf(
        ExchangeCardUM.Empty(
            titleReference = resourceReference(id = R.string.swapping_from_title),
        ),
        ExchangeCardUM.Filled(
            titleReference = resourceReference(id = R.string.swapping_from_title),
            tokenItemState = TokenItemState.Content(
                id = "1",
                iconState = CurrencyIconState.Locked,
                titleState = TokenItemState.TitleState.Content(text = "Bitcoin"),
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
        ),
    )
}