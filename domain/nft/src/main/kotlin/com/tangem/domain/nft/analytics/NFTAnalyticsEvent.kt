package com.tangem.domain.nft.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.COLLECTIONS
import com.tangem.core.analytics.models.AnalyticsParam.Key.NFT
import com.tangem.core.analytics.models.AnalyticsParam.Key.NO_COLLECTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.STANDARD
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATE
import com.tangem.domain.nft.models.NFTAsset

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
        class ScreenOpened : NFTAnalyticsEvent(event = "Receive NFT Screen Opened")

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
            private val assetId: NFTAsset.Identifier,
        ) : NFTAnalyticsEvent(
            event = "NFT Details Screen Opened",
            params = buildMap {
                put(BLOCKCHAIN, blockchain)
                assetId.standard()?.let {
                    put(STANDARD, it)
                }
            },
        )

        class ButtonReadMore : NFTAnalyticsEvent(event = "Button - Read More")
        class ButtonSeeAll : NFTAnalyticsEvent(event = "Button - See All")
        class ButtonExplore : NFTAnalyticsEvent(event = "Button - Explore")
        class ButtonSend : NFTAnalyticsEvent(event = "Button - Send")
    }
}

private fun NFTAsset.Identifier.standard(): String? = when (this) {
    is NFTAsset.Identifier.EVM -> contractType.name
    is NFTAsset.Identifier.Solana -> tokenStandard?.let { METAPLEX_INDEX_BY_SOLANA_STANDARD[it] ?: it }
    is NFTAsset.Identifier.TON -> null
    is NFTAsset.Identifier.Unknown -> null
}

/**
 * Legacy Solana standards are reported as the Metaplex `TokenStandard` index the old Moralis endpoint returns, as
 * analytics has always received them; standards that have no index (Core) go by name.
 */
private val METAPLEX_INDEX_BY_SOLANA_STANDARD = mapOf(
    "Metaplex" to "0",
    "MetaplexEdition" to "3",
    "ProgrammableNft" to "4",
    "ProgrammableNftEdition" to "5",
)