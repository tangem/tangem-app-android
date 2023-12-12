package com.tangem.feature.swap.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.states.PercentLowerThanBest
import com.tangem.feature.swap.models.states.ProviderState

/**
 * UI Item for swap provider wrapped in a [BaseContainer] with rounded corners
 *
 * https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=7856-41909&mode=design&t=vo7dyElitnzSPSW3-4
 */

private const val GRAY_SCALE_SATURATION = 0f
private const val GRAY_SCALE_ALPHA = 0.4f
private val GrayscaleColorFilter: ColorFilter
    get() = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(GRAY_SCALE_SATURATION) })

@Composable
fun ProviderItemBlock(state: ProviderState, modifier: Modifier = Modifier) {
    if (state !is ProviderState.Empty) {
        BaseContainer(modifier = modifier) {
            ProviderItem(
                state = state,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
    }
}

@Composable
fun ProviderItem(state: ProviderState, modifier: Modifier = Modifier, isSelected: Boolean = false) {
    when (state) {
        is ProviderState.Content -> {
            ProviderContentState(
                state = state,
                modifier = modifier,
                isSelected = isSelected,
            )
        }
        is ProviderState.Loading -> {
            ProviderLoadingState(
                modifier = modifier,
            )
        }
        is ProviderState.Unavailable -> {
            ProviderUnavailableState(
                state = state,
                modifier = modifier,
                isSelected = isSelected,
            )
        }
        is ProviderState.Empty -> {
            // do nothing
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ProviderContentState(
    state: ProviderState.Content,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row {
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
                    AnimatedContent(targetState = state.name, label = "") {
                        Text(
                            text = it,
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.primary1,
                        )
                    }
                    AnimatedContent(targetState = state.type, label = "") {
                        Text(
                            text = it,
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                            modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                        )
                    }
                    when (state.additionalBadge) {
                        ProviderState.AdditionalBadge.BestTrade ->
                            BestTradeItem(Modifier.padding(start = TangemTheme.dimens.spacing4))
                        ProviderState.AdditionalBadge.PermissionRequired ->
                            PermissionBadgeItem(Modifier.padding(start = TangemTheme.dimens.spacing4))
                        ProviderState.AdditionalBadge.Empty -> {
                            // no-op
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(
                        top = TangemTheme.dimens.spacing8,
                        end = TangemTheme.dimens.spacing56,
                    ),
                ) {
                    AnimatedContent(targetState = state.subtitle, label = "") {
                        Text(
                            text = it.resolveReference(),
                            style = TangemTheme.typography.body2,
                            color = TangemTheme.colors.text.tertiary,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                    if (state.percentLowerThenBest is PercentLowerThanBest.Value &&
                        state.percentLowerThenBest.value > 0
                    ) {
                        AnimatedContent(targetState = state.percentLowerThenBest.value, label = "") {
                            Text(
                                text = "-$it%",
                                style = TangemTheme.typography.body2,
                                color = TangemTheme.colors.text.warning,
                                modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        ProviderChevron(selectionType = state.selectionType, isSelected = isSelected)
    }
}

@Composable
private fun ProviderUnavailableState(
    state: ProviderState.Unavailable,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row {
            val (alpha, colorFilter) = GRAY_SCALE_ALPHA to GrayscaleColorFilter
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
                alpha = alpha,
                colorFilter = colorFilter,
                contentDescription = null,
            )

            Column(
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing12),
            ) {
                Row {
                    AnimatedContent(targetState = state.name, label = "") {
                        Text(
                            text = it,
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                        )
                    }
                    AnimatedContent(targetState = state.type, label = "") {
                        Text(
                            text = it,
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                            modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                        )
                    }
                }
                AnimatedContent(targetState = state.alertText, label = "") {
                    Text(
                        text = it.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                        modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
                    )
                }
            }
        }

        ProviderChevron(selectionType = state.selectionType, isSelected = isSelected)
    }
}

@Composable
private fun ProviderLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column {
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
                    strokeWidth = TangemTheme.dimens.size2,
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
private fun BoxScope.ProviderChevron(selectionType: ProviderState.SelectionType, isSelected: Boolean) {
    when (selectionType) {
        ProviderState.SelectionType.NONE -> {
            /* no-op */
        }
        ProviderState.SelectionType.CLICK -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                contentDescription = null,
                modifier = Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .padding(end = TangemTheme.dimens.spacing12),
                tint = TangemTheme.colors.icon.informative,
            )
        }
        ProviderState.SelectionType.SELECT -> {
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_24),
                    contentDescription = null,
                    modifier = Modifier
                        .align(alignment = Alignment.CenterEnd)
                        .padding(end = TangemTheme.dimens.spacing12),
                    tint = TangemTheme.colors.icon.accent,
                )
            }
        }
    }
}

@Composable
private fun BaseContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .fillMaxWidth()
            .defaultMinSize(minHeight = TangemTheme.dimens.size68),
    ) {
        content()
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
            text = "Best rate",
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.icon.accent,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing6),
        )
    }
}

@Composable
private fun PermissionBadgeItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.background.secondary,
            shape = TangemTheme.shapes.roundedCornersLarge,
        ),
    ) {
        Text(
            text = stringResource(id = R.string.express_provider_permission_needed),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing6),
        )
    }
}

@Preview
@Composable
private fun ProviderItem_Loading_Preview() {
    Column {
        TangemTheme(isDark = false) {
            ProviderItemBlock(state = ProviderState.Loading())
        }

        SpacerH24()

        TangemTheme(isDark = true) {
            ProviderItemBlock(state = ProviderState.Loading())
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
        subtitle = stringReference("1 000 000"),
        additionalBadge = ProviderState.AdditionalBadge.PermissionRequired,
        percentLowerThenBest = PercentLowerThanBest.Value(-1.0f),
        selectionType = ProviderState.SelectionType.SELECT,
        onProviderClick = {},
    )
    Column {
        TangemTheme(isDark = false) {
            ProviderItemBlock(state = state)
        }

        SpacerH24()

        TangemTheme(isDark = true) {
            ProviderItemBlock(state = state)
        }
    }
}

@Preview
@Composable
private fun ProviderItem_Unavailable_Preview() {
    val state = ProviderState.Unavailable(
        id = "1",
        name = "1inch",
        type = "DEX",
        iconUrl = "",
        selectionType = ProviderState.SelectionType.SELECT,
        alertText = stringReference("Unavailable"),
    )
    Column {
        TangemTheme(isDark = false) {
            ProviderItemBlock(state = state)
        }

        SpacerH24()

        TangemTheme(isDark = true) {
            ProviderItemBlock(state = state)
        }

        SpacerH24()

        TangemTheme(isDark = true) {
            ProviderItem(state = state, isSelected = true)
        }
    }
}