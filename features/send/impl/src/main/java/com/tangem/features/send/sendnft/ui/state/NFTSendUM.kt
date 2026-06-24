package com.tangem.features.send.sendnft.ui.state

import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.common.ui.state.ConfirmUM

internal data class NFTSendUM(
    val destinationUM: DestinationUM,
    val feeSelectorUM: FeeSelectorUM,
    val confirmUM: ConfirmUM,
    val navigationUM: NavigationUM,
)