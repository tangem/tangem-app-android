package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM.Content.CollectionPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WalletNFTItem2(state: WalletNFTItemUM, modifier: Modifier = Modifier) {
    val nftModifier = modifier
        .clip(RoundedCornerShape(18.dp))
        .background(TangemTheme.colors2.surface.level3)
    when (state) {
        is WalletNFTItemUM.Hidden -> Unit
        is WalletNFTItemUM.Empty -> WalletNFTItemEmpty(
            modifier = nftModifier,
            onClick = state.onItemClick,
        )
        is WalletNFTItemUM.Failed -> WalletNFTItemFailed(modifier = nftModifier)
        is WalletNFTItemUM.Loading -> WalletNFTItemLoading(modifier = nftModifier)

        is WalletNFTItemUM.Content -> WalletNFTItemContent(
            state = state,
            onClick = state.onItemClick,
            modifier = nftModifier,
        )
    }
}

@Composable
private fun WalletNFTItemEmpty(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier.clickableSingle(
            onClick = onClick,
        ),
    ) {
        Image(
            painter = painterResource(R.drawable.img_nft_empty_collection),
            contentDescription = null,
            modifier = Modifier
                .size(TangemTheme.dimens2.x10)
                .layoutId(TangemRowLayoutId.HEAD),
        )
        Text(
            text = stringResourceSafe(R.string.nft_wallet_title),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_TOP)
                .padding(start = TangemTheme.dimens2.x2),
        )
        Text(
            text = stringResourceSafe(R.string.nft_wallet_receive_nft),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_BOTTOM)
                .padding(start = TangemTheme.dimens2.x2),
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_small_right_24),
            contentDescription = null,
            tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
            modifier = Modifier.layoutId(TangemRowLayoutId.TAIL),
        )
    }
}

@Composable
private fun WalletNFTItemContent(state: WalletNFTItemUM.Content, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier.clickableSingle(
            onClick = onClick,
        ),
    ) {
        Box(modifier = Modifier.layoutId(TangemRowLayoutId.HEAD)) {
            CollectionsPreviews(
                previews = state.previews,
            )
        }
        Text(
            text = stringResourceSafe(R.string.nft_wallet_title),
            style = TangemTheme.typography2.bodySemibold16.applyBladeBrush(
                isEnabled = state.isFlickering,
                textColor = TangemTheme.colors2.text.neutral.primary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_TOP)
                .padding(horizontal = TangemTheme.dimens2.x2),
        )
        Text(
            text = stringResourceSafe(
                id = R.string.nft_wallet_count,
                state.allAssetsCount,
                state.collectionsCount,
            ),
            style = TangemTheme.typography2.captionSemibold12.applyBladeBrush(
                isEnabled = state.isFlickering,
                textColor = TangemTheme.colors2.text.neutral.secondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_BOTTOM)
                .padding(horizontal = TangemTheme.dimens2.x2),
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_small_right_24),
            contentDescription = null,
            tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
            modifier = Modifier.layoutId(TangemRowLayoutId.TAIL),
        )
    }
}

@Composable
private fun WalletNFTItemFailed(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .size(TangemTheme.dimens2.x10)
                .clip(RoundedCornerShape(TangemTheme.dimens2.x3))
                .background(TangemTheme.colors2.skeleton.backgroundPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens2.x5),
                painter = painterResource(R.drawable.ic_error_sync_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
        Text(
            text = stringResourceSafe(R.string.nft_wallet_title),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_TOP)
                .padding(start = TangemTheme.dimens2.x2),
        )
        Text(
            text = stringResourceSafe(R.string.nft_wallet_unable_to_load),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_BOTTOM)
                .padding(start = TangemTheme.dimens2.x2),
        )
    }
}

@Composable
private fun WalletNFTItemLoading(modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens2.x10)
                .clip(RoundedCornerShape(TangemTheme.dimens2.x3))
                .background(TangemTheme.colors2.skeleton.backgroundPrimary)
                .layoutId(TangemRowLayoutId.HEAD),
        )
        TextShimmer(
            style = TangemTheme.typography2.bodySemibold16,
            radius = TangemTheme.dimens2.x25,

            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_TOP)
                .padding(start = TangemTheme.dimens2.x2)
                .width(TangemTheme.dimens.size110),
        )
        TextShimmer(
            style = TangemTheme.typography2.captionSemibold12,
            radius = TangemTheme.dimens2.x25,
            modifier = Modifier
                .layoutId(TangemRowLayoutId.START_BOTTOM)
                .padding(start = TangemTheme.dimens2.x2)
                .width(TangemTheme.dimens.size80),
        )
    }
}

@Composable
@Suppress("MagicNumber", "ReusedModifierInstance")
private fun BoxScope.CollectionsPreviews(previews: ImmutableList<CollectionPreview>, modifier: Modifier = Modifier) {
    val modifiers = when (previews.size) {
        1 -> previews1Modifiers()
        2 -> previews2Modifiers()
        3 -> previews3Modifiers()
        else -> previews4Modifiers()
    }
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x10),
    ) {
        previews.take(modifiers.size).forEachIndexed { index, s ->
            val previewModifier = modifiers[index]
            when (s) {
                is CollectionPreview.Image -> {
                    SubcomposeAsyncImage(
                        modifier = previewModifier,
                        model = s.url,
                        loading = {
                            RectangleShimmer()
                        },
                        error = {
                            Box(
                                modifier = previewModifier.background(TangemTheme.colors2.surface.level2),
                            )
                        },
                        contentDescription = null,
                    )
                }
                is CollectionPreview.More -> {
                    Icon(
                        modifier = previewModifier
                            .background(TangemTheme.colors2.surface.level2),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_nft_preview_more_16),
                        tint = TangemTheme.colors2.text.neutral.secondary,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun previews1Modifiers(): List<Modifier> = listOf(
    Modifier
        .size(TangemTheme.dimens2.x10)
        .clip(RoundedCornerShape(TangemTheme.dimens2.x3)),
)

@Composable
private fun BoxScope.previews2Modifiers(): List<Modifier> = listOf(
    Modifier
        .padding(start = TangemTheme.dimens2.x0_5, top = TangemTheme.dimens2.x0_5)
        .size(TangemTheme.dimens2.x6)
        .clip(RoundedCornerShape(TangemTheme.dimens2.x2))
        .align(Alignment.TopStart),
    Modifier
        .zIndex(1f)
        .padding(TangemTheme.dimens2.x0_5)
        .clip(RoundedCornerShape(topStart = 10.dp))
        .background(TangemTheme.colors2.surface.level3)
        .padding(start = TangemTheme.dimens2.x0_5, top = TangemTheme.dimens2.x0_5)
        .size(TangemTheme.dimens2.x6)
        .clip(RoundedCornerShape(TangemTheme.dimens2.x2))
        .align(Alignment.BottomEnd),
)

@Composable
private fun BoxScope.previews3Modifiers(): List<Modifier> = listOf(
    Modifier
        .padding(start = 3.dp, top = 3.dp)
        .size(17.8.dp)
        .clip(RoundedCornerShape(6.dp))
        .align(Alignment.TopStart),
    Modifier
        .zIndex(1f)
        .padding(top = 10.dp, end = 1.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(TangemTheme.colors2.surface.level3)
        .padding(TangemTheme.dimens2.x0_5)
        .size(18.dp)
        .clip(RoundedCornerShape(6.dp))
        .align(Alignment.TopEnd),
    Modifier
        .padding(start = 9.dp, top = 2.dp)
        .size(14.dp)
        .clip(RoundedCornerShape(4.dp))
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
private fun Preview_WalletNFTItem(@PreviewParameter(WalletNFTItemProvider2::class) state: WalletNFTItemUM) {
    TangemThemePreviewRedesign {
        WalletNFTItem2(
            state = state,
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1),
        )
    }
}

private class WalletNFTItemProvider2 : CollectionPreviewParameterProvider<WalletNFTItemUM>(
    collection = listOf(
        WalletNFTItemUM.Empty(
            onItemClick = { },
        ),
        WalletNFTItemUM.Loading,
        WalletNFTItemUM.Failed,
        WalletNFTItemUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
            ),
            allAssetsCount = 125,
            collectionsCount = 11,
            noCollectionAssetsCount = 0,
            isFlickering = true,
            onItemClick = { },
        ),
        WalletNFTItemUM.Content(
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
        WalletNFTItemUM.Content(
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
        WalletNFTItemUM.Content(
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
        WalletNFTItemUM.Content(
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