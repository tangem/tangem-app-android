package com.tangem.domain.pay.model

import java.math.BigDecimal

data class AccountFee(
    val type: String,
    val name: String,
    val paymentMethodId: String?,
    val group: AccountFeeGroupType,
    val amount: BigDecimal,
    val currency: String,
)