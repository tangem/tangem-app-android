package com.tangem.managetokens.presentation.managetokens.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.managetokens.presentation.managetokens.state.QuotesState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.state.previewdata.TokenItemStatePreviewData

private val TOKEN_ITEM_HEIGHT: Dp
    @Composable
    @ReadOnlyComposable
    get() = TangemTheme.dimens.size68

@Composable
internal fun TokenRowItem(state: TokenItemState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenItemState.Loading -> LoadingTokenItem(modifier)
        is TokenItemState.Loaded -> LoadedTokenItem(state, modifier)
    }
}

@Composable
private fun LoadedTokenItem(state: TokenItemState.Loaded, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = TOKEN_ITEM_HEIGHT)
            .background(TangemTheme.colors.background.primary),
        contentAlignment = Alignment.CenterStart,
    ) {
        val width = maxWidth

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        ) {
            TokenIcon(state = state.tokenIcon)
            SpacerW12()

            Column(
                modifier = Modifier
                    .weight(weight = 1f),
            ) {
                TokenName(name = state.name, currencyId = state.currencySymbol)
                TokenPriceData(price = state.rate, quotesState = state.quotes)
            }
            SpacerW24()

            if (width > TangemTheme.dimens.size350 && // hide chart for small screens
                state.quotes is QuotesState.Content
            ) {
                Chart(quotes = state.quotes)
                SpacerW24()
            }

            TokenButton(
                type = state.availableAction.value,
                onClick = { state.onButtonClick(state) },
            )
        }
    }
}

@Composable
private fun TokenName(name: String, currencyId: String) {
    Row {
        Text(
            text = name,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(weight = 1f, fill = false),
        )
        SpacerW(width = TangemTheme.dimens.spacing6)
        Text(
            text = currencyId,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TokenPriceData(price: String?, quotesState: QuotesState) {
    if (price != null) {
        Row {
            Text(
                text = price,
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(weight = 1f, fill = false),
            )
            SpacerW(width = TangemTheme.dimens.spacing6)
            TokenPriceChange(state = quotesState)
        }
    }
}

@Composable
private fun Chart(quotes: QuotesState.Content) {
    Box(
        modifier = Modifier
            .size(width = TangemTheme.dimens.size50, height = TangemTheme.dimens.size28),
    ) {
        PriceChangesChart(values = quotes.chartData)
    }
}

@Composable
private fun LoadingTokenItem(modifier: Modifier = Modifier) {
    BaseSurface(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TangemTheme.dimens.spacing16,
                    vertical = TangemTheme.dimens.spacing4,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            CircleShimmer(modifier = Modifier.size(size = TangemTheme.dimens.size36))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing10)) {
                    RectangleShimmer(
                        modifier = Modifier.size(
                            width = TangemTheme.dimens.size70,
                            height = TangemTheme.dimens.size12,
                        ),
                    )
                    RectangleShimmer(
                        modifier = Modifier.size(
                            width = TangemTheme.dimens.size52,
                            height = TangemTheme.dimens.size12,
                        ),
                    )
                }
                RectangleShimmer(
                    modifier = Modifier.size(
                        width = TangemTheme.dimens.size46,
                        height = TangemTheme.dimens.size12,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BaseSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = TOKEN_ITEM_HEIGHT),
        color = TangemTheme.colors.background.primary,
    ) {
        content()
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Tokens(@PreviewParameter(TokenConfigProvider::class) state: TokenItemState) {
    TangemThemePreview {
        TokenRowItem(state)
    }
}

private class TokenConfigProvider : CollectionPreviewParameterProvider<TokenItemState>(
    collection = listOf(
        TokenItemStatePreviewData.tokenLoading,
        TokenItemStatePreviewData.loadedPriceDown,
        TokenItemStatePreviewData.loadedPriceUp,
        TokenItemStatePreviewData.loadedPriceNeutral,
    ),
)
// endregion Preview
