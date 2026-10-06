package com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees

import arrow.core.Either
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayOnrampFeesUMConverter(
    private val onRetryClick: () -> Unit,
) : Converter<Either<VisaApiError, List<AccountFee>>, TangemPayOnrampFeesUM> {

    override fun convert(value: Either<VisaApiError, List<AccountFee>>): TangemPayOnrampFeesUM = value.fold(
        ifLeft = { TangemPayOnrampFeesUM.Error(onRetryClick = onRetryClick) },
        ifRight = { fees -> TangemPayOnrampFeesUM.Content(rows = fees.map(::toFeeRow).toImmutableList()) },
    )

    private fun toFeeRow(fee: AccountFee): TangemPayOnrampFeesUM.FeeRow {
        val currency = getJavaCurrencyByCode(fee.currency)
        return TangemPayOnrampFeesUM.FeeRow(
            type = fee.type,
            title = stringReference(fee.name),
            value = fee.amount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() },
        )
    }
}