package com.tangem.domain.wallets.models

import java.math.BigDecimal

data class ParsedQrCode(
    val address: String,
    val memo: String? = null,
    val amount: BigDecimal? = null,
)