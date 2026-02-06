package com.tangem.features.feed.model.market.list.state

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.feed.impl.R

internal sealed class MarketsNotificationUM(val config: NotificationConfig) {

    data class YieldSupplyPromo(
        val onClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : MarketsNotificationUM(
        config = NotificationConfig(
            iconResId = R.drawable.img_yield_supply_in_market_notification,
            title = resourceReference(R.string.markets_yield_supply_banner_title),
            subtitle = TextReference.EMPTY,
            onClick = onClick,
            onCloseClick = onCloseClick,
        ),
    )
}