package com.tangem.features.tangempay.tiers

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import org.joda.time.format.DateTimeFormatter

fun TangemPayTariffPlan.formatRecurringFeeOrNull(): String? {
    val fee = fees.find { it.type == TangemPayTariffPlan.Fee.Type.RECURRING } ?: return null
    val currency = getJavaCurrencyByCode(fee.currency)
    return fee.amount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
}

fun TangemPayCustomerTariffPlan.formatNextBillingDateOrNull(
    formatter: DateTimeFormatter = DateTimeFormatters.dateMMMMdYYYY,
): String? {
    return nextBillingAt?.let { DateTimeFormatters.formatDate(it, formatter) }
}