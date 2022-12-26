@file:OptIn(ExperimentalFoundationApi::class)

package com.tangem.feature.swap.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.CurrencyPlaceholderIcon
import com.tangem.core.ui.components.SpacerW2
import com.tangem.core.ui.components.appbar.ExpandableSearchView
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelect
import com.tangem.feature.swap.presentation.R
import com.valentinilk.shimmer.shimmer

@Composable
fun SwapSelectTokenScreen(state: SwapSelectTokenStateHolder, onBack: () -> Unit) {

    TangemTheme {
        Scaffold(
            content = {
                ListOfTokens(state = state)
            },
            topBar = {
                ExpandableSearchView(
                    title = stringResource(R.string.swapping_token_list_your_title),
                    onBackClick = onBack,
                    placeholderSearchText = stringResource(id = R.string.search_tokens_title),
                    onSearchChanged = state.onSearchEntered,
                    onSearchDisplayClosed = { state.onSearchEntered("") },
                )
            },
        )
    }
}

@Composable
private fun ListOfTokens(state: SwapSelectTokenStateHolder) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.background.primary)
            .padding(
                start = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing32,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {

        stickyHeader {
            Header(title = R.string.swapping_token_list_your_tokens)
        }


        itemsIndexed(items = state.addedTokens) { index, item ->
            TokenItem(token = item, onTokenClick = { state.onTokenSelected(item.id) })

            if (index != state.addedTokens.lastIndex) Divider(
                color = TangemTheme.colors.stroke.primary,
                startIndent = TangemTheme.dimens.spacing54,
            )
        }

        stickyHeader {
            Header(title = R.string.swapping_token_list_other_tokens)
        }

        itemsIndexed(items = state.otherTokens) { index, item ->
            TokenItem(token = item, onTokenClick = { state.onTokenSelected(item.id) })
            if (index != state.otherTokens.lastIndex) Divider(
                color = TangemTheme.colors.stroke.primary,
                startIndent = TangemTheme.dimens.spacing54,
            )
        }
    }
}

@Composable
private fun Header(@StringRes title: Int) {
    Text(
        text = stringResource(id = title).uppercase(),
        style = TangemTheme.typography.overline,
        color = TangemTheme.colors.text.tertiary,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = TangemTheme.colors.background.primary)
            .padding(vertical = TangemTheme.dimens.spacing6),
        textAlign = TextAlign.Start,

        )
}

@Composable
private fun TokenItem(
    token: TokenToSelect,
    onTokenClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = TangemTheme.dimens.spacing14,
                bottom = TangemTheme.dimens.spacing14,
                end = TangemTheme.dimens.spacing16,
            )
            .clickable(
                onClick = { onTokenClick(token.id) },
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(end = TangemTheme.dimens.spacing12)
                .align(Alignment.CenterVertically),
        ) {
            val iconModifier = Modifier
                .size(TangemTheme.dimens.size40)
            SubcomposeAsyncImage(
                modifier = iconModifier,
                model = ImageRequest.Builder(LocalContext.current)
                    .data(token.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = token.id,
                loading = { TokenImageShimmer(modifier = iconModifier) },
                error = { CurrencyPlaceholderIcon(modifier = iconModifier, id = token.id) },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically),
        ) {
            Text(
                text = token.name,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerW2()
            Text(
                text = token.symbol,
                style = TangemTheme.typography.caption,
                color = TangemTheme.colors.text.tertiary,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (token.addedTokenBalanceData != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing8),
            ) {
                Text(
                    text = token.addedTokenBalanceData.amount ?: "",
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerW2()
                Text(
                    text = token.addedTokenBalanceData.amountEquivalent ?: "",
                    style = TangemTheme.typography.caption,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }

    }
}

@Composable
private fun TokenImageShimmer(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.shimmer()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = TangemTheme.colors.button.secondary,
                    shape = CircleShape,
                ),
        )
    }
}

private val token = TokenToSelect(
    id = "",
    name = "USDC",
    symbol = "USDC",
    iconUrl = "",
    addedTokenBalanceData = TokenBalanceData(
        amount = "15 000 $",
        amountEquivalent = "15 000 " +
            "USDT",
    ),
)

@Preview
@Composable
fun TokenScreenPreview() {
    SwapSelectTokenScreen(
        state = SwapSelectTokenStateHolder(
            listOf(token, token, token), listOf(token, token, token), {}, {},
        ),
    ) {

    }
}
