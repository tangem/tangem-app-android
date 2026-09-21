package com.tangem.features.tangempay.withdraw.bank.amount.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import com.tangem.domain.pay.model.bankofframp.OfframpEta
import com.tangem.domain.pay.model.bankofframp.OfframpFee
import com.tangem.features.tangempay.details.impl.R
import java.math.BigDecimal
import java.util.Currency

/** The fees catalog names the rail ("ACH", "FedWire"); the built-in labels are the fallback. */
internal fun OfframpDestination.toTitleReference(): TextReference =
    fee.name?.takeIf(String::isNotBlank)?.let(::stringReference) ?: rail.toDefaultTitleReference()

private fun BankRail.toDefaultTitleReference(): TextReference = when (this) {
    BankRail.ACH -> resourceReference(R.string.tangempay_bank_transfer_fee_ach)
    BankRail.WIRE -> resourceReference(R.string.tangempay_bank_transfer_fee_fedwire)
}

internal fun OfframpEta.toTextReference(): TextReference = when {
    maxDays == 0 -> resourceReference(R.string.tangempay_withdraw_bank_eta_same_business_day)
    minDays == maxDays -> pluralReference(
        id = R.plurals.tangempay_withdraw_bank_eta_business_days,
        count = maxDays,
        formatArgs = wrappedList(maxDays),
    )
    else -> resourceReference(R.string.tangempay_order_type_delivery_eta_range, wrappedList(minDays, maxDays))
}

internal fun OfframpFee.toText(): String = amount.formatFiat(Currency.getInstance(currency))

internal fun BigDecimal.formatFiat(currency: Currency): String = format {
    fiat(fiatCurrencyCode = currency.currencyCode, fiatCurrencySymbol = currency.symbol).optionalDecimals()
}