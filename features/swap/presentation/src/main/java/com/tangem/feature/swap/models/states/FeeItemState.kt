package com.tangem.feature.swap.models.states

import com.tangem.feature.swap.domain.models.ui.FeeType

data class FeeItemState(
    val feeType: FeeType,
    val title: String,
    val amountCrypto: String,
    val symbolCrypto: String,
    val amountFiat: String,
    val symbolFiat: String,
    val onClick: () -> Unit,
)