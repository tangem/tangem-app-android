package com.tangem.features.send.v2.api.entry

import com.tangem.core.decompose.navigation.Route

/**
 * Route for switching send and send via swap flows.
 */
sealed class SendEntryRoute : Route {

    /** Route to send screen */
    data object Send : SendEntryRoute()
    /** Route to send via swap screen */
    data object SendWithSwap : SendEntryRoute()
    /** Route to choose token screen for send via swap */
    data class ChooseToken(
        val showSendViaSwapNotification: Boolean,
    ) : SendEntryRoute()
}