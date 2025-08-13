package com.tangem.features.send.v2.entrypoint

import com.tangem.core.decompose.navigation.Route

internal sealed class SendEntryRoute : Route {
    data object Send : SendEntryRoute()
    data object SendWithSwap : SendEntryRoute()
    data class ChooseToken(
        val showSendViaSwapNotification: Boolean,
    ) : SendEntryRoute()
}