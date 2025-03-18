package com.tangem.features.send.v2.send.ui.state

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM

internal data class SendUM(
    val amountUM: AmountState,
    val destinationUM: DestinationUM,
)