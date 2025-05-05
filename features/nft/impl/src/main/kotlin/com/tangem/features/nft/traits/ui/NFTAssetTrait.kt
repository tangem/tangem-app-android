package com.tangem.features.nft.traits.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.traits.entity.NFTAssetTraitUM

@Composable
internal fun NFTAssetTrait(state: NFTAssetTraitUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = state.name,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
        Text(
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing4),
            text = state.value,
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
    }
}