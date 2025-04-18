package com.tangem.features.nft.traits.entity

import kotlinx.collections.immutable.ImmutableList

data class NFTAssetTraitsUM(
    val onBackClick: () -> Unit,
    val traits: ImmutableList<NFTAssetTraitUM>,
)