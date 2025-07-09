package com.tangem.features.nft.details.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTDetails(state: NFTDetailsUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    Box(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            TangemTopAppBar(
                modifier = Modifier,
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_back_24,
                    onIconClicked = state.onBackClick,
                ),
                title = state.nftAsset.name,
            )

            TangemPullToRefreshContainer(
                config = state.pullToRefreshConfig,
                modifier = Modifier.fillMaxSize(),
            ) {
                NFTDetailsAsset(
                    state = state.nftAsset,
                    onReadMoreClick = state.onReadMoreClick,
                    onSeeAllTraitsClick = state.onSeeAllTraitsClick,
                    onExploreClick = state.onExploreClick,
                )
            }
        }

        PrimaryButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing16,
                )
                .fillMaxWidth(),
            text = stringResourceSafe(id = R.string.common_send),
            onClick = state.onSendClick,
        )
    }
}