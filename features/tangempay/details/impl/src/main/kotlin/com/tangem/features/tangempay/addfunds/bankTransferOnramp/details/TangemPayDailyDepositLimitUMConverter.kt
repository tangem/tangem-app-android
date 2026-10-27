package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.account.BankCredentials
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.details.TangemPayBankTransferOnrampDetailsScreenUM.DailyDepositLimitUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.BANK_CREDENTIALS_CURRENCY_CODE
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.formatFiatAmount
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter

internal class TangemPayDailyDepositLimitUMConverter : Converter<BankCredentials.DepositLimit, DailyDepositLimitUM?> {

    override fun convert(value: BankCredentials.DepositLimit): DailyDepositLimitUM? {
        if (value.period != BankCredentials.DepositLimit.Period.DAY) return null
        val formattedAmount = value.amount.formatFiatAmount(currencyCode = BANK_CREDENTIALS_CURRENCY_CODE)
        return DailyDepositLimitUM(
            value = stringReference(formattedAmount),
            hint = resourceReference(
                id = R.string.tangempay_bank_transfer_details_daily_deposits_hint,
                formatArgs = wrappedList(formattedAmount),
            ),
        )
    }
}