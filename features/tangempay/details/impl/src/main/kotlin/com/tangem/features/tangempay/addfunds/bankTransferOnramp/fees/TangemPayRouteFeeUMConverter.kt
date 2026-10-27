package com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.BankCredentials
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal const val BANK_CREDENTIALS_CURRENCY_CODE = "USD"

internal object TangemPayRouteFeeUMConverter : Converter<BankCredentials, TangemPayOnrampFeesUM> {

    override fun convert(value: BankCredentials): TangemPayOnrampFeesUM =
        TangemPayOnrampFeesUM.Content(rows = listOfNotNull(value.toFeeRow()).toImmutableList())

    private fun BankCredentials.toFeeRow(): TangemPayOnrampFeesUM.FeeRow? {
        val railName = rail ?: return null
        val fee = depositFee ?: return null
        val value = when (fee.type) {
            BankCredentials.DepositFee.Type.FLAT -> fee.amount.formatFiatAmount(BANK_CREDENTIALS_CURRENCY_CODE)
            BankCredentials.DepositFee.Type.PERCENTAGE -> fee.amount.formatPercentageFee()
            BankCredentials.DepositFee.Type.UNKNOWN -> return null
        }
        return TangemPayOnrampFeesUM.FeeRow(type = railName, title = stringReference(railName), value = value)
    }
}