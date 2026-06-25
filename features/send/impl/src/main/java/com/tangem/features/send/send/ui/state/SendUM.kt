package com.tangem.features.send.send.ui.state

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.send.confirm.model.ConfirmData

internal data class SendUM(
    val amountUM: AmountState,
    val destinationUM: DestinationUM,
    val feeSelectorUM: FeeSelectorUM,
    val confirmUM: ConfirmUM,
    val confirmData: ConfirmData?,
)