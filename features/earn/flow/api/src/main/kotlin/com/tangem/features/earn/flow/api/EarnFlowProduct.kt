package com.tangem.features.earn.flow.api

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnLegal
import com.tangem.features.earn.product.api.product.EarnProvider

/**
 * What the flow takes from a product: neither the registry nor the product itself is needed. [legal] `null` hides the
 * agreement line on the confirmation.
 */
data class EarnFlowProduct(
    val productId: EarnProductId,
    val title: TextReference,
    val provider: EarnProvider,
    val analytics: EarnProductAnalytics,
    val howItWorks: EarnHowItWorks? = null,
    val legal: EarnLegal? = null,
)