package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.GRAY_SCALE_ALPHA
import com.tangem.core.ui.utils.GrayscaleColorFilter
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState

/**
 * UI Item for swap provider wrapped with rounded corners
 *
 * https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=7856-41909&mode=design&t=vo7dyElitnzSPSW3-4
 */

@Composable
fun ProviderItemBlock(state: ProviderState, modifier: Modifier = Modifier) {
    if (state !is ProviderState.Empty) {
        ProviderItem(
            state = state,
            modifier = modifier
                .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                )
                .clickable(
                    enabled = state.onProviderClick != null,
                    onClick = { state.onProviderClick?.invoke(state.id) },
                )
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens.spacing12),
        )
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

// Be careful when will replace with InputRowBestRate, because RecommendedBadge was added
@Deprecated("Replace with InputRowBestRate")
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
                model = ImageRequest.Builder(context = LocalContext.current).data(state.iconUrl)
                    .crossfade(enable = true).allowHardware(false).build(),
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
                    if (state.namePrefix == ProviderState.PrefixType.PROVIDED_BY) {
                        Text(
                            text = stringResource(id = R.string.express_by_provider),
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                            modifier = Modifier.padding(end = TangemTheme.dimens.spacing4),
                        )
                    }
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
                    val badgeModifier = Modifier.padding(start = TangemTheme.dimens.spacing4)
                    when (state.additionalBadge) {
                        ProviderState.AdditionalBadge.BestTrade -> BestTradeItem(badgeModifier)
                        ProviderState.AdditionalBadge.PermissionRequired -> PermissionBadgeItem(badgeModifier)
                        ProviderState.AdditionalBadge.Recommended -> RecommendedItem(badgeModifier)
                        ProviderState.AdditionalBadge.Empty -> Unit
                    }
                }
                Row(
                    modifier = Modifier.padding(
                        top = TangemTheme.dimens.spacing6,
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
                    if (state.percentLowerThenBest is PercentDifference.Value &&
                        state.percentLowerThenBest.value != 0f
                    ) {
                        val textColor = if (state.percentLowerThenBest.value > 0) {
                            TangemTheme.colors.icon.accent
                        } else {
                            TangemTheme.colors.text.warning
                        }
                        AnimatedContent(targetState = state.percentLowerThenBest.value, label = "") {
                            Text(
                                text = if (it > 0) "+$it%" else "$it%",
                                style = TangemTheme.typography.body2,
                                color = textColor,
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
                model = ImageRequest.Builder(context = LocalContext.current).data(state.iconUrl)
                    .crossfade(enable = true).allowHardware(false).build(),
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
                        modifier = Modifier.padding(top = TangemTheme.dimens.spacing6),
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
                text = stringResource(R.string.express_provider),
                style = TangemTheme.typography.subtitle2,
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
                    modifier = Modifier
                        .size(TangemTheme.dimens.size16)
                        .align(Alignment.CenterVertically),
                    color = TangemTheme.colors.icon.informative,
                    strokeWidth = TangemTheme.dimens.size2,
                )
                Text(
                    text = stringResource(R.string.express_fetch_best_rates),
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
        ProviderState.SelectionType.NONE -> { /* no-op */
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
private fun ErrorProviderIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
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
            text = stringResource(R.string.express_provider_best_rate),
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

@Composable
private fun RecommendedItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
            shape = TangemTheme.shapes.roundedCornersLarge,
        ),
    ) {
        Text(
            text = stringResource(id = R.string.express_provider_recommended),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.icon.accent,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing6),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProviderItemPreview(
    @PreviewParameter(ProviderItemParameterProvider::class) state: Pair<ProviderState, Boolean>,
) {
    TangemThemePreview {
        ProviderItem(
            modifier = Modifier.background(TangemTheme.colors.background.action),
            state = state.first,
            isSelected = state.second,
        )
    }
}

private class ProviderItemParameterProvider : CollectionPreviewParameterProvider<Pair<ProviderState, Boolean>>(
    collection = buildList {
        val contentState = ProviderState.Content(
            id = "1",
            name = "1inch",
            type = "DEX",
            iconUrl = "",
            subtitle = stringReference(value = "0,64554846 DAI ≈ 1 MATIC"),
            additionalBadge = ProviderState.AdditionalBadge.Empty,
            percentLowerThenBest = PercentDifference.Value(value = 12.0f),
            selectionType = ProviderState.SelectionType.SELECT,
            namePrefix = ProviderState.PrefixType.PROVIDED_BY,
            onProviderClick = {},
        )
        val contentState2 = contentState.copy(
            subtitle = stringReference(value = "1 132,46 MATIC"),
            additionalBadge = ProviderState.AdditionalBadge.PermissionRequired,
            percentLowerThenBest = PercentDifference.Value(value = 5f),
        )
        val unavailableState = ProviderState.Unavailable(
            id = "1",
            name = "1inch",
            type = "DEX",
            iconUrl = "",
            alertText = stringReference(value = "Not available"),
            selectionType = ProviderState.SelectionType.SELECT,
            onProviderClick = {},
        )
        val loadingState = ProviderState.Loading()

        add(contentState to true)
        add(contentState to false)

        add(contentState2 to true)
        add(contentState2 to false)

        add(unavailableState to true)
        add(unavailableState to false)

        add(loadingState to true)
        add(loadingState to false)
    },
)
// endregion Preview