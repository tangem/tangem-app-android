package com.tangem.features.nft.traits.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.impl.R
import com.tangem.features.nft.traits.entity.NFTAssetTraitsUM

@Composable
internal fun NFTAssetTraits(state: NFTAssetTraitsUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary),
    ) {
        TangemTopAppBar(
            modifier = Modifier,
            startButton = TopAppBarButtonUM.Back(
                onBackClicked = state.onBackClick,
            ),
            title = stringResourceSafe(R.string.nft_traits_title),
        )

        NFTAssetTraitsContent(
            state = state,
        )
    }
}