package com.tangem.features.swap.v2.impl.sendviaswap.entity

import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM

internal data class SendWithSwapUM(
    val amountUM: SwapAmountUM,
    val destinationUM: DestinationUM,
    val feeSelectorUM: FeeSelectorUM,
    val confirmUM: ConfirmUM,
    val navigationUM: NavigationUM,
)