package com.tangem.feature.swap.preview

import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.swap.domain.models.ui.FeeType
import com.tangem.feature.swap.models.states.FeeItemState

object FeeItemStatePreview {

    val state = FeeItemState.Content(
        feeType = FeeType.NORMAL,
        title = stringReference("Fee"),
        amountCrypto = "1000",
        symbolCrypto = "MATIC",
        amountFiatFormatted = "(1000$)",
        isClickable = false,
        onClick = {},
    )

    val stateClickable = state.copy(isClickable = true)
}
