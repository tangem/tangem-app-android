package com.tangem.features.earn.flow.api

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.product.EarnProvider

/** What the flow takes from a product: neither the registry nor the product itself is needed. */
data class EarnFlowProduct(
    val productId: EarnProductId,
    val title: TextReference,
    val provider: EarnProvider,
    val analytics: EarnProductAnalytics,
)