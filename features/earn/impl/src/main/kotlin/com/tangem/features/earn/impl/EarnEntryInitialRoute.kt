package com.tangem.features.earn.impl

import com.tangem.features.earn.api.EarnEntryStart
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.navigation.EarnRoute

internal fun EarnEntryStart.toInitialRoute(): EarnRoute = when (this) {
    is EarnEntryStart.Picker -> EarnPickerRoute(preselected = preselected, redirectToActive = false)
    is EarnEntryStart.Info -> EarnPickerRoute(preselected = productId, redirectToActive = false)
    EarnEntryStart.Auto -> EarnPickerRoute(preselected = null, redirectToActive = true)
}