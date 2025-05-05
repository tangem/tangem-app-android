package com.tangem.features.nft.collections.entity

import androidx.annotation.DrawableRes
import com.tangem.core.ui.extensions.TextReference

internal data class NFTCollectionUM(
    val id: String,
    val name: String?,
    @DrawableRes val networkIconId: Int,
    val logoUrl: String?,
    val description: TextReference,
    val assets: NFTCollectionAssetsListUM,
    val isExpanded: Boolean,
    val onExpandClick: () -> Unit,
)