package com.tangem.features.nft.traits.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
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

    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.secondary,
        topBar = {
            TangemTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_back_24,
                    onIconClicked = state.onBackClick,
                ),
                title = stringResourceSafe(R.string.nft_traits_title),
            )
        },
        content = { innerPadding ->
            NFTAssetTraitsContent(
                modifier = Modifier
                    .padding(innerPadding),
                state = state,
            )
        },
    )
}