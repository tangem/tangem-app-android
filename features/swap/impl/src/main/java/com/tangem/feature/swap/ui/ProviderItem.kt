package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.GRAY_SCALE_ALPHA
import com.tangem.core.ui.utils.GrayscaleColorFilter
import com.tangem.core.ui.utils.selectedBorder
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState

/**
 * UI Item for swap provider wrapped with rounded corners
 *
 * https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=7856-41909&mode=design&t=vo7dyElitnzSPSW3-4
 */

@Composable
fun ProviderItemBlock(state: ProviderState, modifier: Modifier = Modifier, isSelected: Boolean = false) {
    if (state !is ProviderState.Empty) {
        ProviderItem(
            state = state,
            modifier = if (isSelected) {
                modifier.selectedBorder()
            } else {
                modifier.clip(RoundedCornerShape(16.dp))
            }
                .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
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
fun ProviderItem(state: ProviderState, modifier: Modifier = Modifier) {
    when (state) {
        is ProviderState.Content -> {
            ProviderContentState(
                state = state,
                modifier = modifier,
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
            )
        }
        is ProviderState.Empty -> {
            // do nothing
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ProviderContentState(state: ProviderState.Content, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                modifier = Modifier
                    .padding(start = TangemTheme.dimens.spacing12)
                    .weight(1F),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row {
                    if (state.namePrefix == ProviderState.PrefixType.SWAP_WITH) {
                        Text(
                            text = stringResourceSafe(id = R.string.express_swap_with),
                            style = TangemTheme.typography.subtitle2,
                            color = TangemTheme.colors.text.tertiary,
                            modifier = Modifier.padding(end = TangemTheme.dimens.spacing4),
                        )
                    }
                    AnimatedContent(targetState = state.name, label = "") {
                        Text(
                            text = it,
                            style = TangemTheme.typography.body2,
                            color = TangemTheme.colors.text.primary1,
                        )
                    }
                    AnimatedContent(targetState = state.type, label = "") {
                        Text(
                            text = it,
                            style = TangemTheme.typography.body2,
                            color = TangemTheme.colors.text.tertiary,
                            modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                        )
                    }
                }
                Row {
                    with(state.details) {
                        rating?.let {
                            OutlinedText(
                                text = it.toString(),
                                iconResId = R.drawable.ic_star_outline_12,
                                modifier = Modifier.padding(end = TangemTheme.dimens.spacing4),
                            )
                        }
                        averageDuration?.let {
                            OutlinedText(
                                text = it.toString(), // TODO
                                iconResId = R.drawable.ic_speed_outline_12,
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                modifier = Modifier.padding(end = TangemTheme.dimens.spacing12),
            ) {
                AnimatedContent(targetState = state.subtitle, label = "") {
                    if (state.selectionType == ProviderState.SelectionType.SELECT) {
                        Text(
                            text = it.resolveReference(),
                            style = TangemTheme.typography.body2,
                            color = TangemTheme.colors.text.tertiary,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }
                val badgeModifier = Modifier.padding(start = TangemTheme.dimens.spacing4)
                when (state.additionalBadge) {
                    ProviderState.AdditionalBadge.BestTrade -> BestTradeItem(badgeModifier)
                    ProviderState.AdditionalBadge.PermissionRequired -> PermissionBadgeItem(badgeModifier)
                    ProviderState.AdditionalBadge.Recommended -> RecommendedItem(badgeModifier)
                    ProviderState.AdditionalBadge.Empty -> {
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
                                    style = TangemTheme.typography.caption2,
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
        }
    }
}

@Composable
private fun ProviderUnavailableState(state: ProviderState.Unavailable, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            style = TangemTheme.typography.subtitle2,
                            color = TangemTheme.colors.text.tertiary,
                        )
                    }
                    AnimatedContent(targetState = state.type, label = "") {
                        Text(
                            text = it,
                            style = TangemTheme.typography.body2,
                            color = TangemTheme.colors.text.tertiary,
                            modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
                        )
                    }
                }
                AnimatedContent(targetState = state.alertText, label = "") {
                    Text(
                        text = it.resolveReference(),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                        modifier = Modifier.padding(top = TangemTheme.dimens.spacing6),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            Text(
                text = stringResourceSafe(R.string.express_provider),
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
                    text = stringResourceSafe(R.string.express_fetch_best_rates),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier.padding(start = TangemTheme.dimens.spacing4),
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
    ProviderBadge(
        textResId = R.string.express_provider_best_rate,
        backgroundColor = TangemTheme.colors.icon.accent,
        textColor = TangemTheme.colors.text.constantWhite,
        modifier = modifier,
    )
}

@Composable
private fun PermissionBadgeItem(modifier: Modifier = Modifier) {
    ProviderBadge(
        textResId = R.string.express_provider_permission_needed,
        backgroundColor = TangemTheme.colors.background.secondary,
        textColor = TangemTheme.colors.text.tertiary,
        modifier = modifier,
    )
}

@Composable
private fun RecommendedItem(modifier: Modifier = Modifier) {
    ProviderBadge(
        textResId = R.string.express_provider_recommended,
        backgroundColor = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
        textColor = TangemTheme.colors.icon.accent,
        modifier = modifier,
    )
}

@Composable
private fun ProviderBadge(textResId: Int, backgroundColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Text(
        text = stringResourceSafe(textResId),
        style = TangemTheme.typography.caption1,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(vertical = 1.dp, horizontal = 6.dp),
    )
}

@Composable
private fun RowScope.OutlinedText(text: String, @DrawableRes iconResId: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing1),
        modifier = modifier
            .alignByBaseline()
            .heightIn(min = TangemTheme.dimens.size16)
            .border(
                width = TangemTheme.dimens.size1,
                color = TangemTheme.colors.stroke.primary,
                shape = TangemTheme.shapes.roundedCornersSmall2,
            )
            .padding(vertical = 1.dp, horizontal = 4.dp),
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier.size(TangemTheme.dimens.size12),
        )

        Text(
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing2),
            text = text,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.caption1,
            maxLines = 1,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 328)
@Preview(showBackground = true, widthDp = 328, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProviderItemPreview(
    @PreviewParameter(ProviderItemParameterProvider::class) state: Pair<ProviderState, Boolean>,
) {
    TangemThemePreview {
        ProviderItemBlock(
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
            subtitle = stringReference(value = "1 MATIC"),
            additionalBadge = ProviderState.AdditionalBadge.BestTrade,
            percentLowerThenBest = PercentDifference.Value(value = 12.0f),
            selectionType = ProviderState.SelectionType.SELECT,
            namePrefix = ProviderState.PrefixType.SWAP_WITH,
            onProviderClick = {},
            details = ProviderState.ProviderDetails(
                rating = 4.9,
                averageDuration = 145,
            ),
        )
        val contentState2 = contentState.copy(
            subtitle = stringReference(value = "1 132,46 MATIC"),
            additionalBadge = ProviderState.AdditionalBadge.Empty,
            percentLowerThenBest = PercentDifference.Value(value = 5f),
            details = contentState.details.copy(
                rating = null,
                averageDuration = null,
            ),
        )
        val unavailableState = ProviderState.Unavailable(
            id = "1",
            name = "1inch",
            type = "DEX",
            iconUrl = "",
            alertText = stringReference(value = "Not available"),
            selectionType = ProviderState.SelectionType.SELECT,
            onProviderClick = {},
            additionalBadge = ProviderState.AdditionalBadge.Empty,
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