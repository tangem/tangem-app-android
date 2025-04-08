package com.tangem.features.send.v2.sendnft.ui.state

import com.tangem.features.send.v2.common.ui.state.NavigationUM
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM

internal data class NFTSendUM(
    val destinationUM: DestinationUM,
    val feeUM: FeeUM,
    val confirmUM: ConfirmUM,
    val navigationUM: NavigationUM,
)