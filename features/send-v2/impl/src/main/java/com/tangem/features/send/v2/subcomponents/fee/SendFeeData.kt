package com.tangem.features.send.v2.subcomponents.fee

import java.math.BigDecimal

data class SendFeeData(
    val amount: BigDecimal?,
    val destinationAddress: String? = null,
)