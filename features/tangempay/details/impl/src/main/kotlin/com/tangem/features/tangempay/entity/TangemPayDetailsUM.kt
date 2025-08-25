package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig

internal data class TangemPayDetailsUM(
    val topBarConfig: TangemPayDetailsTopBarConfig,
    val pullToRefreshConfig: PullToRefreshConfig,
)