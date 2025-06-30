package com.tangem.features.nft.details.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.atoms.text.ReadMoreText
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.flicker
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.impl.R

private const val READ_MORE_MAX_LINES = 3

@Composable
internal fun NFTDetailsInfoGroup(
    state: NFTAssetUM.TopInfo,
    onReadMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is NFTAssetUM.TopInfo.Empty -> Unit
        is NFTAssetUM.TopInfo.Content -> {
            InformationBlock(
                modifier = modifier,
                title = if (state.title != null) {
                    { NFTDetailsGroupTitle(state.title) }
                } else {
                    null
                },
                contentHorizontalPadding = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            bottom = TangemTheme.dimens.spacing12,
                        ),
                ) {
                    SalePrice(state.salePrice)
                    Description(state.description, onReadMoreClick)
                    Rarity(state.rarity)
                }
            }
        }
    }
}

@Composable
private fun SalePrice(state: NFTAssetUM.SalePrice, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        visible = state !is NFTAssetUM.SalePrice.Empty,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        when (state) {
            is NFTAssetUM.SalePrice.Empty -> Unit
            is NFTAssetUM.SalePrice.Loading -> {
                Column(
                    modifier = modifier
                        .padding(
                            start = TangemTheme.dimens.spacing12,
                            end = TangemTheme.dimens.spacing12,
                        ),
                ) {
                    TextShimmer(
                        modifier = Modifier
                            .width(TangemTheme.dimens.size158),
                        style = TangemTheme.typography.head,
                        textSizeHeight = true,
                    )
                    TextShimmer(
                        modifier = Modifier
                            .padding(top = TangemTheme.dimens.spacing4)
                            .width(TangemTheme.dimens.size90),
                        style = TangemTheme.typography.caption2,
                        textSizeHeight = true,
                    )
                }
            }
            is NFTAssetUM.SalePrice.Content -> {
                Column(
                    modifier = modifier
                        .padding(
                            start = TangemTheme.dimens.spacing12,
                            end = TangemTheme.dimens.spacing12,
                        ),
                    verticalArrangement = Arrangement.SpaceAround,
                ) {
                    Text(
                        text = state.cryptoPrice.resolveReference(),
                        style = TangemTheme.typography.head,
                        color = TangemTheme.colors.text.primary1,
                    )
                    Text(
                        modifier = Modifier
                            .padding(top = TangemTheme.dimens.spacing4)
                            .flicker(state.isFlickering),
                        text = state.fiatPrice.resolveReference(),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Description(description: String?, onReadMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    if (!description.isNullOrEmpty()) {
        val interactionSource = remember { MutableInteractionSource() }
        ReadMoreText(
            modifier = modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onReadMoreClick,
                )
                .padding(
                    start = TangemTheme.dimens.spacing12,
                    top = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing12,
                ),
            expanded = false,
            text = description,
            style = TangemTheme.typography.body2.copy(
                color = TangemTheme.colors.text.tertiary,
            ),
            readMoreText = resourceReference(R.string.common_read_more).resolveReference(),
            readMoreMaxLines = READ_MORE_MAX_LINES,
            readMoreStyle = TangemTheme.typography.body2.copy(
                color = TangemTheme.colors.text.accent,
            ).toSpanStyle(),
        )
    }
}

@Composable
private fun Rarity(state: NFTAssetUM.Rarity, modifier: Modifier = Modifier) {
    when (state) {
        is NFTAssetUM.Rarity.Empty -> Unit
        is NFTAssetUM.Rarity.Content -> {
            if (state.showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = TangemTheme.dimens.spacing12,
                        top = TangemTheme.dimens.spacing8,
                        end = TangemTheme.dimens.spacing12,
                    ),
                    color = TangemTheme.colors.stroke.primary,
                )
            }
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(
                        start = TangemTheme.dimens.spacing6,
                        top = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing6,
                    ),
            ) {
                NFTDetailsGroupBlock(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = TangemTheme.dimens.spacing6,
                        ),
                    title = resourceReference(R.string.nft_details_rarity_label),
                    value = stringReference(state.label),
                    showInfoButton = true,
                    onBlockClick = state.onLabelClick,
                )
                NFTDetailsGroupBlock(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            horizontal = TangemTheme.dimens.spacing6,
                        ),
                    title = resourceReference(R.string.nft_details_rarity_rank),
                    value = stringReference(state.rank),
                    showInfoButton = true,
                    onBlockClick = state.onRankClick,
                )
            }
        }
    }
}

@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTDetailsInfoBlock(@PreviewParameter(NFTAssetInfoProvider::class) state: NFTAssetUM.TopInfo) {
    TangemThemePreview {
        NFTDetailsInfoGroup(
            state = state,
            onReadMoreClick = { },
        )
    }
}

@Suppress("LongMethod", "MagicNumber")
private class NFTAssetInfoProvider : CollectionPreviewParameterProvider<NFTAssetUM.TopInfo>(
    collection = listOf(
        NFTAssetUM.TopInfo.Content(
            title = resourceReference(R.string.nft_details_last_sale_price),
            salePrice = NFTAssetUM.SalePrice.Content(
                cryptoPrice = stringReference("ETH 18.75"),
                fiatPrice = stringReference("$ 1"),
                isFlickering = false,
            ),
            description = "Base edition by Piux. An illustration of Crypto Robot #7804".repeat(3),
            rarity = NFTAssetUM.Rarity.Content(
                rank = "Top 1% rarity",
                label = "115.28",
                showDivider = true,
                onLabelClick = { },
                onRankClick = { },
            ),
        ),
        NFTAssetUM.TopInfo.Content(
            title = resourceReference(R.string.nft_details_last_sale_price),
            salePrice = NFTAssetUM.SalePrice.Loading,
            description = "Base edition by Piux. An illustration of Crypto Robot #7804".repeat(3),
            rarity = NFTAssetUM.Rarity.Content(
                rank = "Top 1% rarity",
                label = "115.28",
                showDivider = true,
                onLabelClick = { },
                onRankClick = { },
            ),
        ),
        NFTAssetUM.TopInfo.Content(
            title = resourceReference(R.string.nft_details_last_sale_price),
            salePrice = NFTAssetUM.SalePrice.Content(
                cryptoPrice = stringReference("ETH 18.75"),
                fiatPrice = stringReference("$ 1"),
                isFlickering = true,
            ),
            description = null,
            rarity = NFTAssetUM.Rarity.Content(
                rank = "Top 1% rarity",
                label = "115.28",
                showDivider = true,
                onLabelClick = { },
                onRankClick = { },
            ),
        ),
        NFTAssetUM.TopInfo.Content(
            title = resourceReference(R.string.nft_details_last_sale_price),
            salePrice = NFTAssetUM.SalePrice.Content(
                cryptoPrice = stringReference("ETH 18.75"),
                fiatPrice = stringReference("$ 1"),
                isFlickering = true,
            ),
            description = "Base edition by Piux. An illustration of Crypto Robot #7804".repeat(3),
            rarity = NFTAssetUM.Rarity.Empty,
        ),
        NFTAssetUM.TopInfo.Content(
            title = resourceReference(R.string.nft_details_last_sale_price),
            salePrice = NFTAssetUM.SalePrice.Content(
                cryptoPrice = stringReference("ETH 18.75"),
                fiatPrice = stringReference("$ 1"),
                isFlickering = true,
            ),
            description = null,
            rarity = NFTAssetUM.Rarity.Empty,
        ),
        NFTAssetUM.TopInfo.Content(
            title = null,
            salePrice = NFTAssetUM.SalePrice.Empty,
            description = "Base edition by Piux. An illustration of Crypto Robot #7804".repeat(3),
            rarity = NFTAssetUM.Rarity.Content(
                rank = "Top 1% rarity",
                label = "115.28",
                showDivider = true,
                onLabelClick = { },
                onRankClick = { },
            ),
        ),
        NFTAssetUM.TopInfo.Content(
            title = null,
            salePrice = NFTAssetUM.SalePrice.Empty,
            description = null,
            rarity = NFTAssetUM.Rarity.Content(
                rank = "Top 1% rarity",
                label = "115.28",
                showDivider = false,
                onLabelClick = { },
                onRankClick = { },
            ),
        ),
        NFTAssetUM.TopInfo.Content(
            title = null,
            salePrice = NFTAssetUM.SalePrice.Empty,
            description = "Base edition by Piux. An illustration of Crypto Robot #7804".repeat(3),
            rarity = NFTAssetUM.Rarity.Empty,
        ),
    ),
)