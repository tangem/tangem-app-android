package com.tangem.domain.nft.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.COLLECTIONS
import com.tangem.core.analytics.models.AnalyticsParam.Key.NFT
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATE

sealed class NFTAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(
    category = "NFT",
    event = event,
    params = params,
) {

    data class NFTListScreenOpened(
        val state: State,
    ) : NFTAnalyticsEvent(
        event = "NFT List Screen Opened",
        params = buildMap {
            put(STATE, state.value)
            if (state is State.Full) {
                put(COLLECTIONS, state.collectionsCount.toString())
                put(NFT, state.assetsCount.toString())
            }
        },
    ) {
        sealed class State(val value: String) {
            data object Empty : State("Empty")
            data class Full(val assetsCount: Int, val collectionsCount: Int) : State("Full")
        }
    }

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
        ) : NFTAnalyticsEvent(event = "NFT Details Screen Opened", params = mapOf(BLOCKCHAIN to blockchain))

        data object ButtonReadMore : NFTAnalyticsEvent(event = "Button - Read More")
        data object ButtonSeeAll : NFTAnalyticsEvent(event = "Button - See All")
        data object ButtonExplore : NFTAnalyticsEvent(event = "Button - Explore")
        data object ButtonSend : NFTAnalyticsEvent(event = "Button - Send")
    }
}