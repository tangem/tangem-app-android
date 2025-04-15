package com.tangem.features.nft.details.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.bottomFade
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.impl.R
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NFTDetailsAsset(
    state: NFTAssetUM,
    onReadMoreClick: () -> Unit,
    onSeeAllTraitsClick: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    val bottomBarHeight = with(LocalDensity.current) {
        WindowInsets.systemBars.getBottom(density = this).toDp()
    }

    val bottomPadding = TangemTheme.dimens.spacing76 + bottomBarHeight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .bottomFade(),
    ) {
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    top = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = bottomPadding,
                )
                .fillMaxSize(),
        ) {
            NFTDetailsLogo(
                state = state.media,
                modifier = Modifier
                    .aspectRatio(1f),
            )
            NFTDetailsInfoGroup(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing12),
                state = state.topInfo,
                onReadMoreClick = onReadMoreClick,
            )
            NFTDetailsBlocksGroup(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing12),
                items = state.traits,
                title = resourceReference(R.string.nft_details_traits),
                action = {
                    NFTBlocksGroupAction(
                        text = resourceReference(R.string.common_see_all),
                        startIcon = { },
                        onClick = onSeeAllTraitsClick,
                    )
                },
            )
            NFTDetailsBlocksGroup(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing12),
                items = state.baseInfoItems,
                title = resourceReference(R.string.nft_details_base_information),
                action = {
                    NFTBlocksGroupAction(
                        text = resourceReference(R.string.common_explore),
                        startIcon = {
                            NFTBlocksGroupActionIcon(iconRes = R.drawable.ic_compass_24)
                        },
                        onClick = onExploreClick,
                    )
                },
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 1000, showBackground = true)
@Preview(widthDp = 360, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTDetailsAssetAsset(@PreviewParameter(NFTAssetProvider::class) state: NFTAssetUM) {
    TangemThemePreview {
        NFTDetailsAsset(
            state = state,
            onReadMoreClick = { },
            onSeeAllTraitsClick = { },
            onExploreClick = { },
        )
    }
}

@Suppress("MagicNumber")
private class NFTAssetProvider : CollectionPreviewParameterProvider<NFTAssetUM>(
    collection = listOf(
        NFTAssetUM(
            name = "Nethers #9744",
            media = NFTAssetUM.Media.Content(
                url = "img1",
                mimetype = "image/jpeg",
            ),
            topInfo = NFTAssetUM.TopInfo.Content(
                title = resourceReference(R.string.nft_details_last_sale_price),
                salePrice = NFTAssetUM.SalePrice.Content(value = "0,012 ETH", fiatValue = "32.34$"),
                description = "Base edition by Piux. An illustration of Crypto Robot #7804".repeat(3),
                rarity = NFTAssetUM.Rarity.Content(
                    rank = "Top 1% rarity",
                    label = "115.28",
                    showDivider = true,
                ),
            ),
            traits = persistentListOf(
                NFTAssetUM.BlockItem(
                    title = stringReference("Tier"),
                    value = "Infinite",
                ),
                NFTAssetUM.BlockItem(
                    title = stringReference("Phygital Toy"),
                    value = "None",
                ),
                NFTAssetUM.BlockItem(
                    title = stringReference("Class"),
                    value = "CYBER",
                ),
                NFTAssetUM.BlockItem(
                    title = stringReference("Accessory"),
                    value = "No accessory",
                ),
                NFTAssetUM.BlockItem(
                    title = stringReference("Sneakers"),
                    value = "Boots",
                ),
                NFTAssetUM.BlockItem(
                    title = stringReference("Artist"),
                    value = "DJ Dragoon",
                ),
            ),
            baseInfoItems = persistentListOf(
                NFTAssetUM.BlockItem(
                    title = resourceReference(R.string.nft_details_token_standard),
                    value = "ERC-721",
                ),
                NFTAssetUM.BlockItem(
                    title = resourceReference(R.string.nft_details_contract_address),
                    value = "0x6811f2fgd892ac83f719",
                    valueTextEllipsis = TextEllipsis.Middle,
                ),
                NFTAssetUM.BlockItem(
                    title = resourceReference(R.string.nft_details_token_id),
                    value = "100200273",
                ),
                NFTAssetUM.BlockItem(
                    title = resourceReference(R.string.nft_details_chain),
                    value = "Ethereum",
                ),
            ),
        ),
    ),
)