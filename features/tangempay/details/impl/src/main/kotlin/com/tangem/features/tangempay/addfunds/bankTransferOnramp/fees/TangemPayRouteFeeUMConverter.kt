package com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.default
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.models.account.BankCredentials
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal const val BANK_CREDENTIALS_CURRENCY_CODE = "USD"
private const val EXCHANGE_RATE_ROW_TYPE = "EXCHANGE_RATE"
private const val EXCHANGE_RATE_DECIMALS = 2

internal object TangemPayRouteFeeUMConverter : Converter<BankCredentials, TangemPayOnrampFeesUM> {

    override fun convert(value: BankCredentials): TangemPayOnrampFeesUM {
        val exchangeRate = value.exchangeRate?.takeIf { value.isSpei }
        return TangemPayOnrampFeesUM.Content(
            rows = listOfNotNull(value.toFeeRow(), exchangeRate?.toRateRow()).toImmutableList(),
            notes = listOfNotNull(
                resourceReference(
                    id = R.string.tangempay_bank_transfer_details_conversion_notice,
                    formatArgs = wrappedList(value.depositCurrencyCode),
                ).takeIf { value.isSpei },
                resourceReference(R.string.tangempay_bank_transfer_details_rate_disclaimer)
                    .takeIf { exchangeRate != null },
            ).toImmutableList(),
        )
    }

    private fun BankCredentials.toFeeRow(): TangemPayOnrampFeesUM.FeeRow? {
        val railName = rail ?: return null
        val fee = depositFee ?: return null
        val value = when (fee.type) {
            BankCredentials.DepositFee.Type.FLAT -> fee.amount.formatFiatAmount(depositCurrencyCode)
            BankCredentials.DepositFee.Type.PERCENTAGE -> fee.amount.formatPercentageFee()
            BankCredentials.DepositFee.Type.UNKNOWN -> return null
        }
        return TangemPayOnrampFeesUM.FeeRow(type = railName, title = stringReference(railName), value = value)
    }

    private fun BankCredentials.ExchangeRate.toRateRow(): TangemPayOnrampFeesUM.FeeRow {
        val formattedRate = rate.format { simple(decimals = EXCHANGE_RATE_DECIMALS).default() }
        return TangemPayOnrampFeesUM.FeeRow(
            type = EXCHANGE_RATE_ROW_TYPE,
            title = resourceReference(R.string.tangempay_bank_transfer_details_exchange_rate),
            value = "1 $base ≈ $formattedRate $quote",
        )
    }
}