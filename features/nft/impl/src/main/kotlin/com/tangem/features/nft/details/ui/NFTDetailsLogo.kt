package com.tangem.features.nft.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTDetailsLogo(state: NFTAssetUM.Media, modifier: Modifier = Modifier) {
    when (state) {
        is NFTAssetUM.Media.Empty -> {
            Placeholder(
                modifier = modifier.fillMaxWidth(),
            )
        }
        is NFTAssetUM.Media.Content -> {
            SubcomposeAsyncImage(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(TangemTheme.shapes.roundedCornersXMedium),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.url)
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
        }
    }
}

@Composable
private fun Placeholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.field.focused),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = Modifier.size(TangemTheme.dimens.size120),
            painter = painterResource(R.drawable.ic_nft_placeholder_120),
            contentDescription = null,
        )
    }
}

@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTDetailsInfoBlock(@PreviewParameter(NFTAssetMediaProvider::class) state: NFTAssetUM.Media) {
    TangemThemePreview {
        NFTDetailsLogo(
            state = state,
            modifier = Modifier
                .aspectRatio(1f),
        )
    }
}

private class NFTAssetMediaProvider : CollectionPreviewParameterProvider<NFTAssetUM.Media>(
    collection = listOf(
        NFTAssetUM.Media.Empty,
        NFTAssetUM.Media.Content(
            url = "img1",
        ),
    ),
)