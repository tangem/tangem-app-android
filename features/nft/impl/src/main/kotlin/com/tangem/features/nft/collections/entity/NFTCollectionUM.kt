package com.tangem.features.nft.collections.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface NFTCollectionItem {
    val id: String
}

internal data class NFTCollectionPortfolioUM(
    override val id: String,
    val title: AccountTitleUM,
) : NFTCollectionItem

internal data class NFTCollectionUM(
    override val id: String,
    val name: String,
    @DrawableRes val networkIconId: Int,
    val logoUrl: String?,
    val description: TextReference,
    val assets: NFTCollectionAssetsListUM,
    val isExpanded: Boolean,
    val onExpandClick: () -> Unit,
) : NFTCollectionItem