package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH2
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.collections.entity.NFTCollectionAssetUM
import com.tangem.features.nft.collections.entity.NFTSalePriceUM
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTCollectionAsset(state: NFTCollectionAssetUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(TangemTheme.shapes.roundedCornersXMedium),
            model = ImageRequest.Builder(LocalContext.current)
                .data(state.imageUrl)
                .crossfade(true)
                .build(),
            loading = {
                RectangleShimmer(radius = TangemTheme.dimens.radius16)
            },
            error = {
                Placeholder()
            },
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        SpacerH12()
        Text(
            text = state.name,
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerH2()
        NFTSalePrice(
            state = state.price,
        )
    }
}

@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.field.focused),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size52),
            painter = painterResource(R.drawable.ic_nft_placeholder_120),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
        )
    }
}

@Preview(widthDp = 180, showBackground = true)
@Preview(widthDp = 180, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTCollectionAsset(
    @PreviewParameter(NFTCollectionAssetProvider::class) state: NFTCollectionAssetUM,
) {
    TangemThemePreview {
        NFTCollectionAsset(
            state = state,
        )
    }
}

private class NFTCollectionAssetProvider : CollectionPreviewParameterProvider<NFTCollectionAssetUM>(
    collection = listOf(
        NFTCollectionAssetUM(
            id = "item1",
            name = "Nethers #0853",
            imageUrl = "img",
            price = NFTSalePriceUM.Loading,
            onItemClick = { },
        ),
        NFTCollectionAssetUM(
            id = "item2",
            name = "Nethers #0854",
            imageUrl = "img",
            price = NFTSalePriceUM.Failed,
            onItemClick = { },
        ),
        NFTCollectionAssetUM(
            id = "item3",
            name = "Nethers #0855",
            imageUrl = "img",
            price = NFTSalePriceUM.Content(
                price = stringReference("0.05 ETH"),
            ),
            onItemClick = { },
        ),
    ),
)