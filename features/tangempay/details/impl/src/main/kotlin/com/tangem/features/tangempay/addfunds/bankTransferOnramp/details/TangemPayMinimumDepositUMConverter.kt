package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.formatFiatAmount
import com.tangem.utils.converter.Converter
import java.math.RoundingMode

internal object TangemPayMinimumDepositUMConverter : Converter<BankCredentials, TextReference?> {

    override fun convert(value: BankCredentials): TextReference? {
        val limits = value.amountLimits ?: return null
        val exchangeRate = value.exchangeRate
            ?.takeIf { rate -> value.isSpei && rate.base.equals(limits.currency, ignoreCase = true) }
        val formattedMinimum = if (exchangeRate != null) {
            limits.min.multiply(exchangeRate.rate)
                .setScale(0, RoundingMode.CEILING)
                .formatFiatAmount(currencyCode = exchangeRate.quote)
        } else {
            limits.min.formatFiatAmount(currencyCode = limits.currency)
        }
        return stringReference(formattedMinimum)
    }
}