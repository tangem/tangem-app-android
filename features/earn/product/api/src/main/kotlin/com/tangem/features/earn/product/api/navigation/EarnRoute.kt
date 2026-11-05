package com.tangem.features.earn.product.api.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.session.EarnFlowAction

sealed interface EarnRoute : Route

data class EarnPickerRoute(
    val preselected: EarnProductId? = null,
    val shouldRedirectToActive: Boolean = false,
) : EarnRoute

data class EarnInfoRoute(val productId: EarnProductId) : EarnRoute

data class EarnFlowRoute(val productId: EarnProductId, val action: EarnFlowAction) : EarnRoute