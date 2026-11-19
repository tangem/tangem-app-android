package com.tangem.features.earn.impl.analytics

import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class EarnBannerAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : EarnAnalyticsEvent(category = EARN_ANALYTICS_CATEGORY, event = event, params = params) {

    class BannerShown(
        token: String,
        blockchain: String,
        bestApy: String?,
        products: Int,
    ) : EarnBannerAnalyticsEvent(
        event = "Earn Banner Shown",
        params = buildMap {
            put(AnalyticsParam.TOKEN_PARAM, token)
            put(AnalyticsParam.BLOCKCHAIN, blockchain)
            bestApy?.let { put(EarnAnalyticsParam.BEST_APY, it) }
            put(EarnAnalyticsParam.PRODUCTS, products.toString())
        },
    )

    class BannerEnable(
        token: String,
        bestApy: String?,
    ) : EarnBannerAnalyticsEvent(
        event = "Button - Earn Banner",
        params = buildMap {
            put(AnalyticsParam.TOKEN_PARAM, token)
            bestApy?.let { put(EarnAnalyticsParam.BEST_APY, it) }
        },
    )

    class YieldRowShown(
        token: String,
        activeCount: Int,
        positionBucket: String?,
    ) : EarnBannerAnalyticsEvent(
        event = "Yield Row Shown",
        params = buildMap {
            put(AnalyticsParam.TOKEN_PARAM, token)
            put(EarnAnalyticsParam.ACTIVE_COUNT, activeCount.toString())
            positionBucket?.let { put(EarnAnalyticsParam.POSITION_BUCKET, it) }
        },
    )

    class YieldRow(
        token: String,
        activeCount: Int,
    ) : EarnBannerAnalyticsEvent(
        event = "Button - Yield Row",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            EarnAnalyticsParam.ACTIVE_COUNT to activeCount.toString(),
        ),
    )

    class YieldSheetOpened(
        token: String,
        activeCount: Int,
        inactiveCount: Int,
    ) : EarnBannerAnalyticsEvent(
        event = "Yield Sheet Opened",
        params = mapOf(
            AnalyticsParam.TOKEN_PARAM to token,
            EarnAnalyticsParam.ACTIVE_COUNT to activeCount.toString(),
            EarnAnalyticsParam.INACTIVE_COUNT to inactiveCount.toString(),
        ),
    )

    class YieldSheetRow(
        product: String,
        isActive: Boolean,
        position: Int,
    ) : EarnBannerAnalyticsEvent(
        event = "Button - Yield Sheet Row",
        params = mapOf(
            EarnAnalyticsParam.PRODUCT to product,
            EarnAnalyticsParam.ROW_STATE to if (isActive) "Active" else "Inactive",
            EarnAnalyticsParam.POSITION to position.toString(),
        ),
    )
}