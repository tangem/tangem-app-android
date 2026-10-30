package com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees

import arrow.core.Either
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.pay.model.AccountFee
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class TangemPayOnrampFeesUMConverter(
    private val onRetryClick: () -> Unit,
) : Converter<Either<VisaApiError, List<AccountFee>>, TangemPayOnrampFeesUM> {

    override fun convert(value: Either<VisaApiError, List<AccountFee>>): TangemPayOnrampFeesUM = value.fold(
        ifLeft = { TangemPayOnrampFeesUM.Error(onRetryClick = onRetryClick) },
        ifRight = { fees -> TangemPayOnrampFeesUM.Content(rows = fees.map(::toFeeRow).toImmutableList()) },
    )

    private fun toFeeRow(fee: AccountFee): TangemPayOnrampFeesUM.FeeRow = TangemPayOnrampFeesUM.FeeRow(
        type = fee.type,
        title = stringReference(fee.name),
        value = when (fee.calculationType) {
            AccountFee.CalculationType.FIXED -> fee.amount.formatFiatAmount(currencyCode = fee.currency)
            AccountFee.CalculationType.PERCENTAGE -> fee.amount.formatPercentageFee()
        },
    )
}

internal fun BigDecimal.formatFiatAmount(currencyCode: String): String {
    val currency = getJavaCurrencyByCode(currencyCode)
    return format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
}

internal fun BigDecimal.formatPercentageFee(): String = movePointLeft(2).format { percent(minFractionDigits = 0) }