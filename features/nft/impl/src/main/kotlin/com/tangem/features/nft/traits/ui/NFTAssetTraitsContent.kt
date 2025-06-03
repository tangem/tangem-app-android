package com.tangem.features.nft.traits.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.traits.entity.NFTAssetTraitUM
import com.tangem.features.nft.traits.entity.NFTAssetTraitsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NFTAssetTraitsContent(state: NFTAssetTraitsUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing16),
        title = null,
        contentHorizontalPadding = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
        ) {
            state.traits.forEach { trait ->
                key(trait.id) {
                    NFTAssetTrait(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = TangemTheme.dimens.spacing12,
                                vertical = TangemTheme.dimens.spacing8,
                            ),
                        state = trait,
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTDetailsAssetAsset(@PreviewParameter(NFTAssetTraitsProvider::class) state: NFTAssetTraitsUM) {
    TangemThemePreview {
        NFTAssetTraitsContent(
            state = state,
        )
    }
}

private class NFTAssetTraitsProvider : CollectionPreviewParameterProvider<NFTAssetTraitsUM>(
    collection = listOf(
        NFTAssetTraitsUM(
            traits = persistentListOf(
                NFTAssetTraitUM(
                    id = "1",
                    name = "Trait 1",
                    value = "Value",
                ),
                NFTAssetTraitUM(
                    id = "2",
                    name = "Trait 2",
                    value = "Value",
                ),
                NFTAssetTraitUM(
                    id = "3",
                    name = "Trait 3",
                    value = "Value",
                ),
                NFTAssetTraitUM(
                    id = "4",
                    name = "Trait 4",
                    value = "Value",
                ),
                NFTAssetTraitUM(
                    id = "5",
                    name = "Trait 5",
                    value = "Value",
                ),
            ),
            onBackClick = { },
        ),
    ),
)