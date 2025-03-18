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
internal fun WalletNFTItem(state: WalletNFTItemUM, modifier: Modifier = Modifier, onClick: () -> Unit = { }) {
    when (state) {
        is WalletNFTItemUM.Hidden -> Unit
        is WalletNFTItemUM.Empty -> WalletNFTItemEmpty(
            modifier = modifier,
            onClick = onClick,
        )
        is WalletNFTItemUM.Failed -> WalletNFTItemFailed(modifier = modifier)
        is WalletNFTItemUM.Loading -> WalletNFTItemLoading(modifier = modifier)

        is WalletNFTItemUM.Content -> WalletNFTItemContent(
            state = state,
            onClick = onClick,
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
                    state.assetsCount,
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
                    .size(TangemTheme.dimens.size36)
                    .clip(RoundedCornerShape(TangemTheme.dimens.radius8))
                    .background(TangemTheme.colors.field.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size20),
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
                    .size(TangemTheme.dimens.size36)
                    .clip(RoundedCornerShape(TangemTheme.dimens.radius8))
                    .background(TangemTheme.colors.field.primary),
            )
        },
        text = {
            TextShimmer(
                modifier = Modifier.width(TangemTheme.dimens.size110),
                style = TangemTheme.typography.caption2,
                textSizeHeight = true,
            )
            TextShimmer(
                modifier = Modifier
                    .width(TangemTheme.dimens.size80)
                    .padding(top = TangemTheme.dimens.spacing4),
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
            .size(TangemTheme.dimens.size36),
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
        .size(TangemTheme.dimens.size36)
        .clip(RoundedCornerShape(TangemTheme.dimens.radius6)),
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
        .background(TangemTheme.colors.background.primary)
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
        .background(TangemTheme.colors.background.primary)
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
        .clip(RoundedCornerShape(TangemTheme.dimens.radius4))
        .size(TangemTheme.dimens.size16)
        .align(Alignment.TopStart),
    Modifier
        .clip(RoundedCornerShape(TangemTheme.dimens.radius4))
        .size(TangemTheme.dimens.size16)
        .align(Alignment.TopEnd),
    Modifier
        .clip(RoundedCornerShape(TangemTheme.dimens.radius4))
        .size(TangemTheme.dimens.size16)
        .align(Alignment.BottomStart),
    Modifier
        .clip(RoundedCornerShape(TangemTheme.dimens.radius4))
        .size(TangemTheme.dimens.size16)
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
        shape = RoundedCornerShape(TangemTheme.dimens.radius16),
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
                    horizontal = TangemTheme.dimens.spacing12,
                    vertical = TangemTheme.dimens.spacing16,
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
                            horizontal = TangemTheme.dimens.spacing12,
                        )
                        .weight(1f),
                    horizontalAlignment = Alignment.Start,
                    content = text,
                )
                Box(
                    modifier = Modifier.heightIn(min = TangemTheme.dimens.size24),
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
private fun Preview_WalletNFTItem_InLight(@PreviewParameter(WalletNFTItemProvider::class) state: WalletNFTItemUM) {
    TangemThemePreview {
        WalletNFTItem(
            state = state,
            onClick = {},
        )
    }
}

private class WalletNFTItemProvider : CollectionPreviewParameterProvider<WalletNFTItemUM>(
    collection = listOf(
        WalletNFTItemUM.Empty,
        WalletNFTItemUM.Loading,
        WalletNFTItemUM.Failed,
        WalletNFTItemUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
            ),
            assetsCount = 125,
            collectionsCount = 11,
            isFlickering = true,
        ),
        WalletNFTItemUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
            ),
            assetsCount = 125,
            collectionsCount = 11,
            isFlickering = false,
        ),
        WalletNFTItemUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
                CollectionPreview.Image("img3"),
            ),
            assetsCount = 125,
            collectionsCount = 11,
            isFlickering = false,
        ),
        WalletNFTItemUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
                CollectionPreview.Image("img3"),
                CollectionPreview.Image("img4"),
            ),
            assetsCount = 125,
            collectionsCount = 11,
            isFlickering = false,
        ),
        WalletNFTItemUM.Content(
            previews = persistentListOf(
                CollectionPreview.Image("img1"),
                CollectionPreview.Image("img2"),
                CollectionPreview.Image("img3"),
                CollectionPreview.More,
            ),
            assetsCount = 125,
            collectionsCount = 11,
            isFlickering = true,
        ),
    ),
)