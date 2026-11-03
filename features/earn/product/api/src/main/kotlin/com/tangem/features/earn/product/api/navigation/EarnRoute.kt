package com.tangem.features.earn.product.api.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.earn.EarnProductId

sealed interface EarnRoute : Route

data class EarnPickerRoute(
    val preselected: EarnProductId? = null,
    val shouldRedirectToActive: Boolean = false,
) : EarnRoute