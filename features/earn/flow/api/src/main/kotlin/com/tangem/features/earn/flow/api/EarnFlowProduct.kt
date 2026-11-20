package com.tangem.features.earn.flow.api

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnProvider

/** What the flow takes from a product: neither the registry nor the product itself is needed. */
data class EarnFlowProduct(
    val productId: EarnProductId,
    val title: TextReference,
    val provider: EarnProvider,
    val analytics: EarnProductAnalytics,
    val howItWorks: EarnHowItWorks? = null,
    val legal: EarnFlowLegal? = null,
)

/** Documents the confirmation screen asks the user to agree with; `null` on [EarnFlowProduct] hides the line. */
data class EarnFlowLegal(val termsOfUseUrl: String, val privacyPolicyUrl: String)