package com.tangem.features.send.v2.send.ui.state

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.features.send.v2.common.NavigationUM
import com.tangem.features.send.v2.send.confirm.ui.state.ConfirmUM
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM

internal data class SendUM(
    val amountUM: AmountState,
    val destinationUM: DestinationUM,
    val feeUM: FeeUM,
    val confirmUM: ConfirmUM,
    val navigationUM: NavigationUM,
)