package com.tangem.feature.swap.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.common.Strings
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.CurrencyPlaceholderIcon
import com.tangem.core.ui.components.SpacerW2
import com.tangem.core.ui.components.appbar.ExpandableSearchView
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.ImageBackgroundContrastChecker
import com.tangem.feature.swap.models.Network
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenBalanceData
import com.tangem.feature.swap.models.TokenToSelect
import com.tangem.feature.swap.presentation.R
import kotlinx.coroutines.launch

@Composable
fun SwapSelectTokenScreen(state: SwapSelectTokenStateHolder, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier
            .systemBarsPadding()
            .background(color = TangemTheme.colors.background.secondary),
        content = { padding ->
            ListOfTokens(state = state, Modifier.padding(padding))
        },
        topBar = {
            ExpandableSearchView(
                title = stringResource(R.string.swapping_token_list_title),
                onBackClick = onBack,
                placeholderSearchText = stringResource(id = R.string.common_search_tokens),
                onSearchChange = state.onSearchEntered,
                onSearchDisplayClose = { state.onSearchEntered("") },
                subtitle = state.network.name,
                icon = painterResource(id = getActiveIconRes(state.network.blockchainId)),
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListOfTokens(state: SwapSelectTokenStateHolder, modifier: Modifier = Modifier) {
    val screenBackgroundColor = TangemTheme.colors.background.secondary
    LazyColumn(
        modifier = modifier
            .background(color = screenBackgroundColor)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.addedTokens.isNotEmpty()) {
            stickyHeader { Header(title = R.string.swapping_token_list_your_tokens) }
        }

        itemsIndexed(items = state.addedTokens) { index, item ->
            TokenItem(
                token = item,
                network = state.network,
                screenBackgroundColor = screenBackgroundColor,
                onTokenClick = {
                    state
                        .onTokenSelected(item.id)
                },
            )

            if (index != state.addedTokens.lastIndex) {
                Divider(
                    color = TangemTheme.colors.stroke.primary,
                    startIndent = TangemTheme.dimens.spacing54,
                )
            }
        }

        if (state.otherTokens.isNotEmpty()) {
            stickyHeader { Header(title = R.string.swapping_token_list_other_tokens) }
        }

        itemsIndexed(items = state.otherTokens) { index, item ->
            TokenItem(
                token = item,
                network = state.network,
                screenBackgroundColor = screenBackgroundColor,
                onTokenClick = { state.onTokenSelected(item.id) },
            )
            if (index != state.otherTokens.lastIndex) {
                Divider(
                    color = TangemTheme.colors.stroke.primary,
                    startIndent = TangemTheme.dimens.spacing54,
                )
            }
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
            .padding(
                vertical = TangemTheme.dimens.spacing6,
                horizontal = TangemTheme.dimens.spacing16,
            ),
        textAlign = TextAlign.Start,
    )
}

@Suppress("LongMethod")
@Composable
private fun TokenItem(token: TokenToSelect, network: Network, screenBackgroundColor: Color, onTokenClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTokenClick)
            .padding(
                vertical = TangemTheme.dimens.spacing14,
                horizontal = TangemTheme.dimens.spacing16,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenIcon(
            token = token,
            screenBackgroundColor = screenBackgroundColor,
            iconPlaceholder = if (token.isNative) getActiveIconRes(network.blockchainId) else null,
        )

        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(
                text = token.name,
                style = TangemTheme.typography.subtitle1,
                color = if (token.available) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.tertiary
                },
            )
            SpacerW2()
            Text(
                text = token.symbol,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!token.available) {
            Text(
                text = stringResource(id = R.string.swapping_token_not_available),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        } else if (token.addedTokenBalanceData != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
            ) {
                Text(
                    text = if (token.addedTokenBalanceData.isBalanceHidden &&
                        !token.addedTokenBalanceData.amountEquivalent.isNullOrEmpty()
                    ) {
                        Strings.STARS
                    } else {
                        token.addedTokenBalanceData.amountEquivalent.orEmpty()
                    },
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                )
                SpacerW2()
                Text(
                    text = if (token.addedTokenBalanceData.isBalanceHidden &&
                        !token.addedTokenBalanceData.amount.isNullOrEmpty()
                    ) {
                        Strings.STARS
                    } else {
                        token.addedTokenBalanceData.amount.orEmpty()
                    },
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun TokenIcon(token: TokenToSelect, screenBackgroundColor: Color, @DrawableRes iconPlaceholder: Int?) {
    var iconBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var isBackgroundColorDefined by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    val data = token.iconUrl.ifEmpty {
        iconPlaceholder
    }
    Box(
        modifier = Modifier
            .padding(end = TangemTheme.dimens.spacing12)
            .background(
                color = iconBackgroundColor,
                shape = TangemTheme.shapes.roundedCorners8,
            ),
    ) {
        val pixelsSize = with(LocalDensity.current) { TangemTheme.dimens.size40.roundToPx() }
        val iconModifier = Modifier.size(TangemTheme.dimens.size40)

        val colorFilter = if (!token.available) {
            val matrix = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(matrix)
        } else {
            null
        }

        SubcomposeAsyncImage(
            modifier = iconModifier,
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .size(size = pixelsSize)
                .memoryCacheKey(key = data.toString() + pixelsSize)
                .crossfade(true)
                .allowHardware(false)
                .listener(
                    onSuccess = { _, result ->
                        if (!isBackgroundColorDefined && isDarkTheme) {
                            coroutineScope.launch {
                                val color = ImageBackgroundContrastChecker(
                                    drawable = result.drawable,
                                    backgroundColor = screenBackgroundColor.toArgb(),
                                    size = pixelsSize,
                                ).getContrastColor(isDarkTheme = true)
                                iconBackgroundColor = color
                                isBackgroundColorDefined = true
                            }
                        }
                    },
                ).build(),
            contentDescription = token.id,
            loading = { CircleShimmer(modifier = iconModifier) },
            error = { CurrencyPlaceholderIcon(modifier = iconModifier, id = token.id) },
            alpha = if (!token.available) 0.7f else 1f,
            colorFilter = colorFilter,
        )
    }
}

private val token = TokenToSelect(
    id = "",
    name = "USDC",
    symbol = "USDC",
    iconUrl = "",
    isNative = false,
    addedTokenBalanceData = TokenBalanceData(
        amount = "15 000 $",
        amountEquivalent = "15 000 " +
            "USDT",
        isBalanceHidden = false,
    ),
)

@Preview
@Composable
private fun TokenScreenPreview() {
    SwapSelectTokenScreen(
        state = SwapSelectTokenStateHolder(
            addedTokens = listOf(token, token, token),
            otherTokens = listOf(token, token, token),
            onSearchEntered = {},
            onTokenSelected = {},
            network = Network("Ethereum", "ETH"),
        ),
        onBack = {},
    )
}