package com.tangem.features.send.api.subcomponents.amount

import com.tangem.core.decompose.navigation.Route

/**
 * Common route for amount
 */
interface AmountRoute : Route {
    val isEditMode: Boolean
}