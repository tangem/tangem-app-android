package com.tangem.features.send.v2.sendnft.ui.state

import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM

internal data class NFTSendUM(
    val destinationUM: DestinationUM,
    val feeUM: FeeUM,
    val feeSelectorUM: FeeSelectorUM,
    val confirmUM: ConfirmUM,
    val navigationUM: NavigationUM,
    val isRedesignEnabled: Boolean,
)