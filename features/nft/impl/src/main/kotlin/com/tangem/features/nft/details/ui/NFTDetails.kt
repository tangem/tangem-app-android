package com.tangem.features.nft.details.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.features.nft.details.entity.NFTInfoBottomSheetConfig
import com.tangem.features.nft.details.ui.bottomsheet.NFTInfoBottomSheet
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTDetails(state: NFTDetailsUM, modifier: Modifier = Modifier) {
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
                title = state.nftAsset.name,
            )
        },
        content = { innerPadding ->
            NFTDetailsAsset(
                state = state.nftAsset,
                onReadMoreClick = state.onReadMoreClick,
                onSeeAllTraitsClick = state.onSeeAllTraitsClick,
                onExploreClick = state.onExploreClick,
                modifier = Modifier
                    .padding(innerPadding),
            )
            ShowBottomSheet(state.bottomSheetConfig)
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            PrimaryButton(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
                text = stringResourceSafe(id = R.string.common_send),
                onClick = state.onSendClick,
            )
        },
    )
}

@Composable
fun ShowBottomSheet(bottomSheetConfig: TangemBottomSheetConfig?) {
    if (bottomSheetConfig == null) return
    when (bottomSheetConfig.content) {
        is NFTInfoBottomSheetConfig -> NFTInfoBottomSheet(bottomSheetConfig)
    }
}