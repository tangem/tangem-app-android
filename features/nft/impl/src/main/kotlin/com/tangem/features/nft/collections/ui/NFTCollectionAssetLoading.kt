package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
internal fun NFTCollectionAssetLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        RectangleShimmer(
            modifier = Modifier
                .aspectRatio(1f),
        )
        SpacerH12()
        TextShimmer(
            modifier = Modifier
                .width(TangemTheme.dimens.size86),
            style = TangemTheme.typography.subtitle2,
            textSizeHeight = true,
        )
        SpacerH2()
        TextShimmer(
            modifier = Modifier
                .width(TangemTheme.dimens.size48),
            style = TangemTheme.typography.caption2,
            textSizeHeight = true,
        )
    }
}

@Preview(widthDp = 180, showBackground = true)
@Preview(widthDp = 180, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTCollectionAsset() {
    TangemThemePreview {
        NFTCollectionAssetLoading()
    }
}