package com.tangem.features.nft.collections.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.collections.entity.NFTCollectionsStateUM
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTCollections(state: NFTCollectionsStateUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary),
    ) {
        AppBarWithBackButton(
            modifier = Modifier,
            onBackClick = state.onBackClick,
            text = stringResourceSafe(id = R.string.nft_collections_title),
            iconRes = R.drawable.ic_back_24,
        )

        TangemPullToRefreshContainer(
            config = state.pullToRefreshConfig,
            modifier = Modifier
                .fillMaxSize(),
        ) {
            when (val content = state.content) {
                is NFTCollectionsUM.Content -> NFTCollectionsContent(content)
                is NFTCollectionsUM.Empty -> NFTCollectionsEmpty(content)
                is NFTCollectionsUM.Failed -> NFTCollectionsFailed(content)
                is NFTCollectionsUM.Loading -> NFTCollectionsLoading(content)
            }
        }
    }
}