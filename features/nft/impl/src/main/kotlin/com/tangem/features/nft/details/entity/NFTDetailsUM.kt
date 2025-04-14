package com.tangem.features.nft.details.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig

internal data class NFTDetailsUM(
    val nftAsset: NFTAssetUM,
    val onBackClick: () -> Unit,
    val onReadMoreClick: () -> Unit,
    val onSeeAllTraitsClick: () -> Unit,
    val onExploreClick: () -> Unit,
    val onSendClick: () -> Unit,
    val bottomSheetConfig: TangemBottomSheetConfig?,
)