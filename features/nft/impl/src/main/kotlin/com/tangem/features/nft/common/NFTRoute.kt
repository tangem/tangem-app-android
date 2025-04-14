package com.tangem.features.nft.common

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

internal sealed class NFTRoute : Route {

    @Serializable
    data class Collections(
        val userWalletId: UserWalletId,
    ) : NFTRoute()

    @Serializable
    data class Receive(
        val userWalletId: UserWalletId,
    ) : NFTRoute()

    @Serializable
    data class Details(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
        val collectionName: String,
    ) : NFTRoute()

    @Serializable
    data class AssetTraits(
        val nftAsset: NFTAsset,
    ) : NFTRoute()
}