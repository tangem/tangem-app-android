package com.tangem.domain.nft.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.COLLECTIONS
import com.tangem.core.analytics.models.AnalyticsParam.Key.NFT
import com.tangem.core.analytics.models.AnalyticsParam.Key.NO_COLLECTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.STANDARD
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATE

sealed class NFTAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(
    category = "NFT",
    event = event,
    params = params,
) {

    data class NFTListScreenOpened(
        val state: AnalyticsParam.EmptyFull,
        val collectionsCount: Int,
        val allAssetsCount: Int,
        val noCollectionAssetsCount: Int,
    ) : NFTAnalyticsEvent(
        event = "NFT List Screen Opened",
        params = buildMap {
            put(STATE, state.value)
            put(COLLECTIONS, collectionsCount.toString())
            put(NFT, allAssetsCount.toString())
            put(NO_COLLECTION, noCollectionAssetsCount.toString())
        },
    )

    object Receive {
        data object ScreenOpened : NFTAnalyticsEvent(event = "Receive NFT Screen Opened")

        data class BlockchainChosen(
            private val blockchain: String,
        ) : NFTAnalyticsEvent(event = "Blockchain Chosen", params = mapOf(BLOCKCHAIN to blockchain))

        data class CopyAddress(
            private val blockchain: String,
        ) : NFTAnalyticsEvent(event = "Button - Copy Address", params = mapOf(BLOCKCHAIN to blockchain))

        data class ShareAddress(
            private val blockchain: String,
        ) : NFTAnalyticsEvent(event = "Button - Share Address", params = mapOf(BLOCKCHAIN to blockchain))
    }

    object Details {
        data class ScreenOpened(
            private val blockchain: String,
            private val standard: String?,
        ) : NFTAnalyticsEvent(
            event = "NFT Details Screen Opened",
            params = buildMap {
                put(BLOCKCHAIN, blockchain)
                standard?.let {
                    put(STANDARD, it)
                }
            },
        )

        data object ButtonReadMore : NFTAnalyticsEvent(event = "Button - Read More")
        data object ButtonSeeAll : NFTAnalyticsEvent(event = "Button - See All")
        data object ButtonExplore : NFTAnalyticsEvent(event = "Button - Explore")
        data object ButtonSend : NFTAnalyticsEvent(event = "Button - Send")
    }
}