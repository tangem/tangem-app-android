package com.tangem.features.send.v2.send.ui.state

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM

internal data class SendUM(
    val amountUM: AmountState,
    val destinationUM: DestinationUM,
    val feeUM: FeeUM,
    val confirmUM: ConfirmUM,
    val navigationUM: NavigationUM,
    val isRedesignEnabled: Boolean,
)