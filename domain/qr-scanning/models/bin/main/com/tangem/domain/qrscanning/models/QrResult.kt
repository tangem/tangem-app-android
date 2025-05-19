package com.tangem.domain.qrscanning.models

import java.math.BigDecimal

data class QrResult(
    var address: String = "",
    var amount: BigDecimal? = null,
    var memo: String? = null,
)