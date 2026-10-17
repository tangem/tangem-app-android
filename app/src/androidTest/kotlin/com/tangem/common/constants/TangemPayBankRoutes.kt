package com.tangem.common.constants

import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.kakao.common.utilities.getResourceString

data class TangemPayBankRoute(
    val rail: String,
    val title: String,
    val fee: String,
    private val etaRes: Int,
    private val etaArgs: List<Any> = emptyList(),
) {
    val eta: String get() = getResourceString(etaRes, *etaArgs.toTypedArray())
}

object TangemPayBankRoutes {

    val ACH = TangemPayBankRoute(
        rail = "ACH",
        title = "ACH",
        fee = "$1.50",
        etaRes = CoreResR.string.tangempay_order_type_delivery_eta_range,
        etaArgs = listOf(1, 3),
    )

    val WIRE = TangemPayBankRoute(
        rail = "WIRE",
        title = "FedWire",
        fee = "$15",
        etaRes = CoreResR.string.tangempay_withdraw_bank_eta_same_business_day,
    )
}