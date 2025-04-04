package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.currency.icon.CurrencyIconTopBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.collections.entity.NFTCollectionAssetsListUM
import com.tangem.features.nft.collections.entity.NFTCollectionUM
import com.tangem.features.nft.impl.R
import kotlinx.collections.immutable.persistentListOf

private const val CHEVRON_ROTATION_EXPANDED = 180f
private const val CHEVRON_ROTATION_COLLAPSED = 0f

@Composable
internal fun NFTCollection(state: NFTCollectionUM, modifier: Modifier = Modifier) {
    val isExpanded = state.isExpanded

    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = state.onExpandClick)
                .padding(
                    vertical = TangemTheme.dimens.spacing16,
                    horizontal = TangemTheme.dimens.spacing12,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Logo(state)

            Text(state)

            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) {
                    CHEVRON_ROTATION_EXPANDED
                } else {
                    CHEVRON_ROTATION_COLLAPSED
                },
                label = "chevron_rotation",
            )

            Icon(
                modifier = Modifier
                    .rotate(rotation)
                    .size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_chevron_24),
                tint = if (isExpanded) {
                    TangemTheme.colors.icon.primary1
                } else {
                    TangemTheme.colors.icon.inactive
                },
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun Logo(state: NFTCollectionUM) {
    val networkBadgeOffset = TangemTheme.dimens.spacing6

    Box(
        modifier = Modifier,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(TangemTheme.dimens.size36)
                .clip(TangemTheme.shapes.roundedCorners8),
            model = ImageRequest.Builder(LocalContext.current)
                .data(state.logoUrl)
                .crossfade(true)
                .build(),
            loading = {
                RectangleShimmer(radius = TangemTheme.dimens.radius8)
            },
            error = {
                Box(
                    modifier = Modifier
                        .clip(shape = TangemTheme.shapes.roundedCorners8)
                        .background(TangemTheme.colors.field.primary),
                )
            },
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        CurrencyIconTopBadge(
            modifier = Modifier
                .offset(x = networkBadgeOffset, y = -networkBadgeOffset)
                .align(Alignment.TopEnd),
            iconResId = state.networkIconId,
            alpha = 1f,
            colorFilter = null,
        )
    }
}

@Composable
private fun RowScope.Text(state: NFTCollectionUM) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(
                horizontal = TangemTheme.dimens.spacing12,
            ),
    ) {
        Text(
            modifier = Modifier,
            text = state.name,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing2),
            text = state.description.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTCollection(@PreviewParameter(NFTCollectionProvider::class) item: NFTCollectionUM) {
    TangemThemePreview {
        NFTCollection(
            state = item,
        )
    }
}

private class NFTCollectionProvider : CollectionPreviewParameterProvider<NFTCollectionUM>(
    collection = listOf(
        NFTCollectionUM(
            id = "item1",
            name = "Nethers",
            logoUrl = "",
            networkIconId = R.drawable.img_eth_22,
            description = TextReference.Str("3 items"),
            assets = NFTCollectionAssetsListUM.Content(persistentListOf()),
            isExpanded = false,
            onExpandClick = { },
        ),
        NFTCollectionUM(
            id = "item2",
            name = "Nethers",
            logoUrl = "",
            networkIconId = R.drawable.img_eth_22,
            description = TextReference.Str("3 items"),
            assets = NFTCollectionAssetsListUM.Loading(
                itemsCount = 3,
            ),
            onExpandClick = { },
            isExpanded = true,
        ),
    ),
)