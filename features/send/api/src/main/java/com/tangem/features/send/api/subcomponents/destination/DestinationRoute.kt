package com.tangem.features.send.api.subcomponents.destination

import com.tangem.core.decompose.navigation.Route

/**
 * Common route for destination
 */
interface DestinationRoute : Route {
    val isEditMode: Boolean
}