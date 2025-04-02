package com.tangem.features.nft.details.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.nft.details.entity.NFTDetailsUM

@Suppress("UnusedPrivateMember")
@Composable
internal fun NFTDetails(state: NFTDetailsUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)
}