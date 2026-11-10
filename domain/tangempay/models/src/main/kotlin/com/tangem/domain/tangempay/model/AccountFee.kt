package com.tangem.domain.tangempay.model

import java.math.BigDecimal

data class AccountFee(
    val type: String,
    val name: String,
    val paymentMethodId: String?,
    val group: AccountFeeGroupType,
    val calculationType: FeeCalculationType,
    val amount: BigDecimal,
    val currency: String,
)