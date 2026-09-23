package com.tangem.features.feed.components.v2.blocks.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.SHTORKA_STATE
import com.tangem.core.analytics.models.OneTimePerSessionEvent

internal sealed class FeedBlocksAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    data class ProductCardShown(
        private val productCard: ProductCard,
        private val shtorkaState: AnalyticsParam.ShtorkaState,
    ) : FeedBlocksAnalyticsEvent(
        event = "Product Card Shown",
        params = mapOf(
            PRODUCT_CARD to productCard.value,
            SHTORKA_STATE to shtorkaState.value,
        ),
    ), OneTimePerSessionEvent {
        override val oneTimeEventId: String = "$event ${productCard.value}"
    }

    data class ProductCardClicked(
        private val productCard: ProductCard,
        private val shtorkaState: AnalyticsParam.ShtorkaState,
    ) : FeedBlocksAnalyticsEvent(
        event = "Product Card Clicked",
        params = mapOf(
            PRODUCT_CARD to productCard.value,
            SHTORKA_STATE to shtorkaState.value,
        ),
    )

    data class ForYouClicked(
        private val shtorkaState: AnalyticsParam.ShtorkaState,
    ) : FeedBlocksAnalyticsEvent(
        event = "For You Clicked",
        params = mapOf(SHTORKA_STATE to shtorkaState.value),
    )
}

internal enum class ProductCard(val value: String) {
    TangemPay("Tangem Pay"),
    Earn("Earn"),
    ReferAFriend("Refer a Friend"),
}

private const val PRODUCT_CARD = "Product Card"