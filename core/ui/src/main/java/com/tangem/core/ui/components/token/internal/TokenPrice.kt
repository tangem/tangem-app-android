package com.tangem.core.ui.components.token.internal

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.SpacerW6
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.core.ui.components.token.state.TokenItemState.SubtitleState as TokenPriceState

@Composable
internal fun TokenPrice(state: TokenPriceState?, modifier: Modifier = Modifier) {
    when (state) {
        is TokenPriceState.CryptoPriceContent -> {
            PriceBlock(
                modifier = modifier,
                price = state.price,
                type = state.type,
                priceChangePercent = state.priceChangePercent,
            )
        }
        is TokenPriceState.TextContent -> {
            PriceText(text = state.value, modifier = modifier, isAvailable = state.isAvailable)
        }
        is TokenPriceState.Unknown -> {
            PriceText(text = DASH_SIGN, modifier = modifier)
        }
        is TokenPriceState.Loading -> {
            RectangleShimmer(modifier = modifier.placeholderSize(), radius = TangemTheme.dimens.radius4)
        }
        is TokenPriceState.Locked -> {
            LockedRectangle(modifier = modifier.placeholderSize())
        }
        null -> Unit
    }
}

@Composable
private fun PriceBlock(
    price: String,
    modifier: Modifier = Modifier,
    type: PriceChangeType? = null,
    priceChangePercent: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PriceText(text = price, modifier = Modifier.weight(weight = 1f, fill = false))

        SpacerW6()

        if (type != null) {
            PriceChangeIcon(type = type)
            SpacerW4()
        }

        PriceChangeText(type = type, text = priceChangePercent)
    }
}

@Composable
private fun PriceText(text: String, modifier: Modifier = Modifier, isAvailable: Boolean = true) {
    AnimatedContent(targetState = text, label = "Update the price text", modifier = modifier) { animatedText ->
        Text(
            text = animatedText,
            color = if (isAvailable) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography.caption2,
        )
    }
}

@Composable
private fun PriceChangeIcon(type: PriceChangeType) {
    AnimatedContent(targetState = type, label = "Update the price change's arrow") { animatedType ->
        Icon(
            painter = painterResource(
                id = when (animatedType) {
                    PriceChangeType.UP -> R.drawable.ic_arrow_up_8
                    PriceChangeType.DOWN -> R.drawable.ic_arrow_down_8
                    PriceChangeType.NEUTRAL -> R.drawable.ic_elipse_8
                },
            ),
            tint = when (animatedType) {
                PriceChangeType.UP -> TangemTheme.colors.icon.accent
                PriceChangeType.DOWN -> TangemTheme.colors.icon.warning
                PriceChangeType.NEUTRAL -> TangemTheme.colors.icon.inactive
            },
            contentDescription = null,
        )
    }
}

@Composable
private fun PriceChangeText(type: PriceChangeType?, text: String?, modifier: Modifier = Modifier) {
    AnimatedContent(targetState = text, modifier = modifier, label = "Update the price change's text") { animatedText ->
        Text(
            text = animatedText ?: DASH_SIGN,
            color = when (type) {
                PriceChangeType.UP -> TangemTheme.colors.text.accent
                PriceChangeType.DOWN -> TangemTheme.colors.text.warning
                PriceChangeType.NEUTRAL -> TangemTheme.colors.text.disabled
                null -> TangemTheme.colors.text.tertiary
            },
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.caption2,
        )
    }
}

private fun Modifier.placeholderSize(): Modifier = composed {
    return@composed this
        .padding(vertical = TangemTheme.dimens.spacing2)
        .size(width = TangemTheme.dimens.size52, height = TangemTheme.dimens.size12)
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(TokenPriceChangeStateProvider::class) state: TokenPriceState) {
    TangemThemePreview {
        TokenPrice(state = state)
    }
}

private class TokenPriceChangeStateProvider : CollectionPreviewParameterProvider<TokenPriceState>(
    collection = listOf(
        TokenPriceState.CryptoPriceContent(
            price = "1.234",
            priceChangePercent = "2.5%",
            type = PriceChangeType.UP,
        ),
        TokenPriceState.CryptoPriceContent(
            price = "1.234",
            priceChangePercent = "2.5%",
            type = PriceChangeType.DOWN,
        ),
        TokenPriceState.CryptoPriceContent(
            price = "1.234",
            priceChangePercent = "2.5%",
            type = PriceChangeType.NEUTRAL,
        ),
        TokenPriceState.TextContent(value = "Subtitle", isAvailable = true),
        TokenPriceState.Unknown,
        TokenPriceState.Loading,
        TokenPriceState.Locked,
    ),
)
