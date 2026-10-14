package com.tangem.feature.wallet.presentation.wallet.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM.Content.CollectionPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun WalletNFTItem(state: WalletNFTItemUM, modifier: Modifier = Modifier) {
    when (state) {
        is WalletNFTItemUM.Hidden -> Unit
        is WalletNFTItemUM.Empty -> WalletNFTItemEmpty(
            modifier = modifier,
            onClick = state.onItemClick,
        )
        is WalletNFTItemUM.Failed -> WalletNFTItemFailed(modifier = modifier)
        is WalletNFTItemUM.Loading -> WalletNFTItemLoading(modifier = modifier)

        is WalletNFTItemUM.Content -> WalletNFTItemContent(
            state = state,
            onClick = state.onItemClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun WalletNFTItemEmpty(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RowContentContainer(
        modifier = modifier,
        icon = {
            Image(
                modifier = Modifier,
                painter = painterResource(R.drawable.ic_nft_empty_36),
                contentDescription = null,
            )
        },
        text = {
            Text(
                text = stringResourceSafe(R.string.nft_wallet_title),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResourceSafe(R.string.nft_wallet_receive_nft),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        action = {
            ChevronArrow()
        },
        onClick = onClick,
    )
}

@Composable
private fun WalletNFTItemContent(state: WalletNFTItemUM.Content, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RowContentContainer(
        modifier = modifier,
        icon = {
            CollectionsPreviews(
                previews = state.previews,
            )
        },
        text = {
            Text(
                text = stringResourceSafe(R.string.nft_wallet_title),
                style = TangemTheme.typography.subtitle2.applyBladeBrush(
                    isEnabled = state.isFlickering,
                    textColor = TangemTheme.colors.text.primary1,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResourceSafe(
                    id = R.string.nft_wallet_count,
                    state.allAssetsCount,
                    state.collectionsCount,
                ),
                style = TangemTheme.typography.caption2.applyBladeBrush(
                    isEnabled = state.isFlickering,
                    textColor = TangemTheme.colors.text.tertiary,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        action = {
            ChevronArrow()
        },
        onClick = onClick,
    )
}

@Composable
private fun WalletNFTItemFailed(modifier: Modifier = Modifier) {
    RowContentContainer(
        modifier = modifier,
        icon = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TangemTheme.colors.field.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_error_sync_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        },
        text = {
            Text(
                text = stringResourceSafe(R.string.nft_wallet_title),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResourceSafe(R.string.nft_wallet_unable_to_load),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        enabled = false,
    )
}

@Composable
private fun WalletNFTItemLoading(modifier: Modifier = Modifier) {
    RowContentContainer(
        modifier = modifier,
        icon = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TangemTheme.colors.field.primary),
            )
        },
        text = {
            TextShimmer(
                modifier = Modifier.width(110.dp),
                style = TangemTheme.typography.caption2,
                textSizeHeight = true,
            )
            TextShimmer(
                modifier = Modifier
                    .width(80.dp)
                    .padding(top = 4.dp),
                style = TangemTheme.typography.caption2,
                textSizeHeight = true,
            )
        },
        enabled = false,
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
        modifier = modifier
            .size(36.dp),
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
                                modifier = Modifier.background(TangemTheme.colors.field.primary),
                            )
                        },
                        contentDescription = null,
                    )
                }
                is CollectionPreview.More -> {
                    Image(
                        modifier = modifier
                            .background(TangemTheme.colors.stroke.primary),
                        painter = painterResource(R.drawable.ic_nft_preview_more_16),
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
        .size(36.dp)
        .clip(RoundedCornerShape(6.dp)),
)

@Composable
private fun BoxScope.previews2Modifiers(): List<Modifier> = listOf(
    Modifier
        .size(22.dp)
        .padding(
            top = 1.dp,
        )
        .clip(RoundedCornerShape(6.dp))
        .align(Alignment.TopStart),
    Modifier
        .zIndex(1f)
        .clip(RoundedCornerShape(6.dp))
        .background(TangemTheme.colors.background.primary)
        .padding(
            top = 2.dp,
            start = 2.dp,
        )
        .size(22.dp)
        .clip(RoundedCornerShape(6.dp))
        .align(Alignment.BottomEnd),
)

@Composable
private fun BoxScope.previews3Modifiers(): List<Modifier> = listOf(
    Modifier
        .padding(
            top = 1.dp,
            bottom = 1.dp,
        )
        .size(16.dp)
        .clip(RoundedCornerShape(5.dp))
        .align(Alignment.TopStart),
    Modifier
        .zIndex(1f)
        .padding(
            top = 4.dp,
        )
        .clip(RoundedCornerShape(6.dp))
        .background(TangemTheme.colors.background.primary)
        .padding(
            start = 2.dp,
            top = 2.dp,
            end = 1.dp,
            bottom = 2.dp,
        )
        .size(18.dp)
        .clip(RoundedCornerShape(6.dp))
        .align(Alignment.TopEnd),
    Modifier
        .padding(
            start = 6.dp,
            bottom = 1.dp,
        )
        .clip(RoundedCornerShape(4.dp))
        .size(14.dp)
        .align(Alignment.BottomStart),
)

@Composable
private fun BoxScope.previews4Modifiers(): List<Modifier> = listOf(
    Modifier
        .clip(RoundedCornerShape(4.dp))
        .size(16.dp)
        .align(Alignment.TopStart),
    Modifier
        .clip(RoundedCornerShape(4.dp))
        .size(16.dp)
        .align(Alignment.TopEnd),
    Modifier
        .clip(RoundedCornerShape(4.dp))
        .size(16.dp)
        .align(Alignment.BottomStart),
    Modifier
        .clip(RoundedCornerShape(4.dp))
        .size(16.dp)
        .align(Alignment.BottomEnd),
)

@Composable
private fun ChevronArrow(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier,
        painter = painterResource(R.drawable.ic_chevron_right_24),
        contentDescription = null,
        tint = TangemTheme.colors.icon.informative,
    )
}

@Composable
private fun RowContentContainer(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    icon: @Composable BoxScope.() -> Unit,
    text: @Composable ColumnScope.() -> Unit,
    action: @Composable BoxScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TangemTheme.colors.background.primary,
            contentColor = TangemTheme.colors.text.primary1,
            disabledContainerColor = TangemTheme.colors.background.primary,
            disabledContentColor = TangemTheme.colors.text.primary1,
        ),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = 12.dp,
                    vertical = 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    content = icon,
                )
                Column(
                    modifier = Modifier
                        .padding(
                            horizontal = 12.dp,
                        )
                        .weight(1f),
                    horizontalAlignment = Alignment.Start,
                    content = text,
                )
                Box(
                    modifier = Modifier.heightIn(min = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                    content = action,
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_WalletNFTItem(@PreviewParameter(WalletNFTItemProvider::class) state: WalletNFTItemUM) {
    TangemThemePreview {
        WalletNFTItem(state = state)
    }
}

private class WalletNFTItemProvider : CollectionPreviewParameterProvider<WalletNFTItemUM>(
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