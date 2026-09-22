package com.tangem.features.feed.crypto.model.entity

import com.tangem.features.feed.crypto.ui.state.MarketIndexUM

internal sealed interface FeedCryptoTabBottomSheetConfig {

    data class MarketIndexInfo(
        val data: MarketIndexUM,
    ) : FeedCryptoTabBottomSheetConfig

    data object MarketInfo : FeedCryptoTabBottomSheetConfig
}