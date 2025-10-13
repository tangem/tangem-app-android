package com.tangem.features.yield.supply.impl.subcomponents.notifications.entity

import com.tangem.domain.transaction.error.GetFeeError
import java.math.BigDecimal

data class YieldSupplyNotificationData(
    val feeValue: BigDecimal?,
    val feeError: GetFeeError?,
)