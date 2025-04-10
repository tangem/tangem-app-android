package com.tangem.features.nft.collections.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.collections.entity.NFTCollectionsStateUM
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTCollections(state: NFTCollectionsStateUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.secondary,
        topBar = {
            AppBarWithBackButton(
                modifier = Modifier.statusBarsPadding(),
                onBackClick = state.onBackClick,
                text = stringResourceSafe(id = R.string.nft_collections_title),
                iconRes = R.drawable.ic_back_24,
            )
        },
        content = { innerPadding ->
            AnimatedContent(
                targetState = state.content,
                contentKey = { it::class },
                label = "NFT Collections",
            ) {
                val contentModifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                when (val content = it) {
                    is NFTCollectionsUM.Content -> NFTCollectionsContent(content, contentModifier)
                    is NFTCollectionsUM.Empty -> NFTCollectionsEmpty(content, contentModifier)
                    is NFTCollectionsUM.Failed -> NFTCollectionsFailed(content, contentModifier)
                    is NFTCollectionsUM.Loading -> Unit
                }
            }
        },
    )
}