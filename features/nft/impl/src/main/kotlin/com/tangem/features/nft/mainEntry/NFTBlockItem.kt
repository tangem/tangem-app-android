package com.tangem.features.nft.mainEntry

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.nft.entity.NFTBlockUM
import com.tangem.features.nft.entity.NFTBlockUM.Content.CollectionPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NFTBlockItem(nftBlockUM: NFTBlockUM, modifier: Modifier = Modifier) {
    when (nftBlockUM) {
        is NFTBlockUM.Content -> NFTBlockItemContent(
            nftBlockUM = nftBlockUM,
            onClick = nftBlockUM.onItemClick,
            modifier = modifier,
        )
        is NFTBlockUM.Empty -> NFTBlockItemEmpty(
            modifier = modifier,
            onClick = nftBlockUM.onItemClick,
        )
        is NFTBlockUM.Failed -> NFTBlockItemFailed(modifier = modifier)
        is NFTBlockUM.Loading -> NFTBlockItemLoading(modifier = modifier)
        is NFTBlockUM.Hidden -> Unit
    }
}

@Composable
private fun NFTBlockItemEmpty(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TangemTheme.colors2.surface.level3)
            .clickableSingle(
                onClick = onClick,
            ),
        content = {
            Image(
                painter = painterResource(R.drawable.ic_nft_empty_40),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x10)
                    .layoutId(TangemRowLayoutId.HEAD),
            )
            Text(
                text = stringResourceSafe(R.string.nft_wallet_title),
                style = TangemTheme.typography2.bodySemibold16,
                color = TangemTheme.colors2.text.neutral.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            )
            Text(
                text = stringResourceSafe(R.string.nft_wallet_receive_nft),
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            )
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_small_right_24),
                tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x6)
                    .layoutId(TangemRowLayoutId.TAIL)
            )
        }
    )
}

@Composable
private fun NFTBlockItemContent(nftBlockUM: NFTBlockUM.Content, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TangemTheme.colors2.surface.level3)
            .clickableSingle(onClick = onClick),
        content = {
            Box(
                modifier = Modifier
                    .padding(end = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x10)
                    .layoutId(TangemRowLayoutId.HEAD)
            ) {
                CollectionsPreviews(previews = nftBlockUM.previews)
            }
            Text(
                text = stringResourceSafe(R.string.nft_wallet_title),
                style = TangemTheme.typography2.bodySemibold16.applyBladeBrush(
                    isEnabled = nftBlockUM.isFlickering,
                    textColor = TangemTheme.colors2.text.neutral.primary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            )
            Text(
                text = stringResourceSafe(
                    id = R.string.nft_wallet_count,
                    nftBlockUM.allAssetsCount,
                    nftBlockUM.collectionsCount,
                ),
                style = TangemTheme.typography2.captionSemibold12.applyBladeBrush(
                    isEnabled = nftBlockUM.isFlickering,
                    textColor = TangemTheme.colors2.text.neutral.secondary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            )
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_small_right_24),
                tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x6)
                    .layoutId(TangemRowLayoutId.TAIL)
            )
        }
    )
}

@Composable
private fun NFTBlockItemFailed(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TangemTheme.colors2.surface.level3),
        content = {
            Icon(
                painter = painterResource(R.drawable.ic_error_sync_24),
                tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
                contentDescription = null,
                modifier = Modifier
                    .size(TangemTheme.dimens2.x10)
                    .clip(RoundedCornerShape(TangemTheme.dimens2.x2))
                    .background(TangemTheme.colors2.graphic.neutral.secondary)
                    .size(TangemTheme.dimens2.x5),
            )
            Box(
                modifier = Modifier
                    .padding(end = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x10)
                    .clip(RoundedCornerShape(TangemTheme.dimens2.x2))
                    .background(TangemTheme.colors2.graphic.neutral.quaternary)
                    .layoutId(TangemRowLayoutId.HEAD),
            )
            Text(
                text = stringResourceSafe(R.string.nft_wallet_title),
                style = TangemTheme.typography2.bodySemibold16,
                color = TangemTheme.colors2.text.status.disabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            )
            Text(
                text = stringResourceSafe(R.string.nft_wallet_unable_to_load),
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.status.disabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            )
        }
    )
}

@Composable
private fun NFTBlockItemLoading(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TangemTheme.colors2.surface.level3),
        content = {
            RectangleShimmer(
                radius = TangemTheme.dimens2.x2,
                modifier = Modifier
                    .padding(end = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x10)
                    .layoutId(TangemRowLayoutId.HEAD),
            )
            TextShimmer(
                text = stringResourceSafe(R.string.nft_wallet_title),
                style = TangemTheme.typography2.bodySemibold16,
                radius = TangemTheme.dimens2.x25,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            )
            TextShimmer(
                text = stringResourceSafe(R.string.nft_wallet_receive_nft),
                style = TangemTheme.typography2.captionSemibold12,
                radius = TangemTheme.dimens2.x25,
                modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            )
        }
    )
}

@Composable
@Suppress("MagicNumber")
private fun BoxScope.CollectionsPreviews(previews: ImmutableList<CollectionPreview>, modifier: Modifier = Modifier) {
    val modifiers = when (previews.size) {
        1 -> previews1Modifiers()
        2 -> previews2Modifiers()
        3 -> previews3Modifiers()
        else -> previews4Modifiers()
    }
    Box(
        modifier = modifier.size(TangemTheme.dimens2.x10),
    ) {
        previews.take(modifiers.size).forEachIndexed { index, s ->
            val modifier = modifiers[index]
            when (s) {
                is CollectionPreview.Image -> {
                    SubcomposeAsyncImage(
                        modifier = modifier,
                        model = s.url,
                        loading = {
                            RectangleShimmer(radius = 0.dp)
                        },
                        error = {
                            Box(
                                modifier = Modifier.background(TangemTheme.colors2.graphic.neutral.tertiaryConstant),
                            )
                        },
                        contentDescription = null,
                    )
                }
                is CollectionPreview.More -> {
                    Icon(
                        modifier = modifier
                            .background(TangemTheme.colors2.surface.level2)
                            .padding(4.dp),
                        painter = painterResource(R.drawable.ic_more_default_24),
                        tint = TangemTheme.colors2.text.neutral.tertiary,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.previews1Modifiers(): List<Modifier> = listOf(
    Modifier
        .size(TangemTheme.dimens2.x10)
        .clip(RoundedCornerShape(6.dp)),
)

@Composable
private fun BoxScope.previews2Modifiers(): List<Modifier> = listOf(
    Modifier
        .size(TangemTheme.dimens.size22)
        .padding(
            top = TangemTheme.dimens.spacing1,
        )
        .clip(RoundedCornerShape(TangemTheme.dimens.radius6))
        .align(Alignment.TopStart),
    Modifier
        .zIndex(1f)
        .clip(RoundedCornerShape(TangemTheme.dimens.radius6))
        .background(TangemTheme.colors2.surface.level3)
        .padding(
            top = TangemTheme.dimens.spacing2,
            start = TangemTheme.dimens.spacing2,
        )
        .size(TangemTheme.dimens.size22)
        .clip(RoundedCornerShape(TangemTheme.dimens.radius6))
        .align(Alignment.BottomEnd),
)

@Composable
private fun BoxScope.previews3Modifiers(): List<Modifier> = listOf(
    Modifier
        .padding(
            top = TangemTheme.dimens.spacing1,
            bottom = TangemTheme.dimens.spacing1,
        )
        .size(TangemTheme.dimens.size16)
        .clip(RoundedCornerShape(5.dp))
        .align(Alignment.TopStart),
    Modifier
        .zIndex(1f)
        .padding(
            top = TangemTheme.dimens.radius4,
        )
        .clip(RoundedCornerShape(TangemTheme.dimens.radius6))
        .background(TangemTheme.colors2.surface.level3)
        .padding(
            start = TangemTheme.dimens.spacing2,
            top = TangemTheme.dimens.spacing2,
            end = TangemTheme.dimens.spacing1,
            bottom = TangemTheme.dimens.spacing2,
        )
        .size(TangemTheme.dimens.size18)
        .clip(RoundedCornerShape(TangemTheme.dimens.radius6))
        .align(Alignment.TopEnd),
    Modifier
        .padding(
            start = TangemTheme.dimens.spacing6,
            bottom = TangemTheme.dimens.spacing1,
        )
        .clip(RoundedCornerShape(TangemTheme.dimens.radius4))
        .size(TangemTheme.dimens.size14)
        .align(Alignment.BottomStart),
)

@Composable
private fun BoxScope.previews4Modifiers(): List<Modifier> = listOf(
    Modifier
        .clip(RoundedCornerShape(6.dp))
        .size(18.dp)
        .align(Alignment.TopStart),
    Modifier
        .clip(RoundedCornerShape(6.dp))
        .size(18.dp)
        .align(Alignment.TopEnd),
    Modifier
        .clip(RoundedCornerShape(6.dp))
        .size(18.dp)
        .align(Alignment.BottomStart),
    Modifier
        .clip(RoundedCornerShape(6.dp))
        .size(18.dp)
        .align(Alignment.BottomEnd),
)

@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_WalletNFTItem(@PreviewParameter(WalletNFTItemProvider::class) state: NFTBlockUM) {
    TangemThemePreviewRedesign {
        NFTBlockItem(nftBlockUM = state)
    }
}

private class WalletNFTItemProvider : CollectionPreviewParameterProvider<NFTBlockUM>(
    collection = listOf(
        NFTBlockUM.Empty(
            onItemClick = { },
        ),
        NFTBlockUM.Loading,
        NFTBlockUM.Failed,
        NFTBlockUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
            ),
            allAssetsCount = 125,
            collectionsCount = 11,
            noCollectionAssetsCount = 0,
            isFlickering = true,
            onItemClick = { },
        ),
        NFTBlockUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
            ),
            allAssetsCount = 125,
            collectionsCount = 11,
            noCollectionAssetsCount = 0,
            isFlickering = false,
            onItemClick = { },
        ),
        NFTBlockUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
                CollectionPreview.Image("img3"),
            ),
            allAssetsCount = 125,
            collectionsCount = 11,
            noCollectionAssetsCount = 0,
            isFlickering = false,
            onItemClick = { },
        ),
        NFTBlockUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
                CollectionPreview.Image("img3"),
                CollectionPreview.Image("img4"),
            ),
            allAssetsCount = 125,
            collectionsCount = 11,
            noCollectionAssetsCount = 0,
            isFlickering = false,
            onItemClick = { },
        ),
        NFTBlockUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
                CollectionPreview.Image("img3"),
                CollectionPreview.More,
            ),
            allAssetsCount = 125,
            collectionsCount = 11,
            isFlickering = true,
            noCollectionAssetsCount = 0,
            onItemClick = { },
        ),
    ),
)