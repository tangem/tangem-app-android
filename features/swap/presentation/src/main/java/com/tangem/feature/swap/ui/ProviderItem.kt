package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.states.ProviderState

/**
 * UI Item for swap provider
 *
 * https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=7856-41909&mode=design&t=vo7dyElitnzSPSW3-4
 */
@Composable
fun ProviderItem(state: ProviderState) {
    when (state) {
        is ProviderState.Content -> {
            ContentProviderState(state = state)
        }
        is ProviderState.Loading -> {
            LoadingProviderState()
        }
    }
}

@Composable
private fun BaseContainer(onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() },
            )
            .fillMaxWidth()
            .defaultMinSize(minHeight = TangemTheme.dimens.size68),
    ) {
        content()
    }
}

@Composable
private fun ContentProviderState(state: ProviderState.Content) {
    BaseContainer(state.onProviderClick) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing12)
                    .size(size = TangemTheme.dimens.size40)
                    .clip(TangemTheme.shapes.roundedCorners8),
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(state.iconUrl)
                    .crossfade(enable = true)
                    .allowHardware(false)
                    .build(),
                loading = { RectangleShimmer(radius = TangemTheme.dimens.radius8) },
                error = {
                    ErrorProviderIcon(
                        Modifier.size(
                            size = TangemTheme.dimens.size40,
                        ),
                    )
                },
                contentDescription = null,
            )

            Column(
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing12),
            ) {
                Row {
                    Text(
                        text = state.name,
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.primary1,
                    )
                    Text(
                        text = state.type,
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                        modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                    )
                    if (state.isBestTrade) {
                        BestTradeItem(Modifier.padding(start = TangemTheme.dimens.spacing4))
                    }
                }
                Text(
                    text = state.rate,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right_24),
            contentDescription = null,
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .padding(end = TangemTheme.dimens.spacing12),
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun LoadingProviderState() {
    BaseContainer {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(vertical = TangemTheme.dimens.spacing12),
        ) {
            Text(
                text = "Provider",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.secondary,
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing12),
            )

            Row(
                modifier = Modifier.padding(
                    top = TangemTheme.dimens.spacing8,
                    start = TangemTheme.dimens.spacing12,
                ),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(TangemTheme.dimens.size16),
                    color = TangemTheme.colors.icon.informative,
                )
                Text(
                    text = "Fetching best rates ...",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right_24),
            contentDescription = null,
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .padding(end = TangemTheme.dimens.spacing12),
            tint = TangemTheme.colors.icon.informative,
        )
    }
}

@Composable
private fun ErrorProviderIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.secondary,
                shape = TangemTheme.shapes.roundedCorners8,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.matchParentSize(),
            painter = painterResource(id = R.drawable.ic_custom_token_44),
            contentDescription = null,
        )
    }
}

@Composable
private fun BestTradeItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
            shape = TangemTheme.shapes.roundedCornersLarge,
        ),
    ) {
        Text(
            text = "Best trade",
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.icon.accent,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing6),
        )
    }
}

@Preview
@Composable
private fun ProviderItem_Loading_Preview() {
    Column {
        TangemTheme(isDark = false) {
            ProviderItem(state = ProviderState.Loading)
        }

        SpacerH24()

        TangemTheme(isDark = true) {
            ProviderItem(state = ProviderState.Loading)
        }
    }
}

@Preview
@Composable
private fun ProviderItem_Content_Preview() {
    val state = ProviderState.Content(
        id = "1",
        name = "1inch",
        type = "DEX",
        iconUrl = "",
        isBestTrade = true,
        rate = "1 000 000",
        onProviderClick = {},
    )
    Column {
        TangemTheme(isDark = false) {
            ProviderItem(state = state)
        }

        SpacerH24()

        TangemTheme(isDark = true) {
            ProviderItem(state = state)
        }
    }
}
