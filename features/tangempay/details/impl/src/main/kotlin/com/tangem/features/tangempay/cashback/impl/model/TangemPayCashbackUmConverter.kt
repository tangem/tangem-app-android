package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackUM
import com.tangem.utils.converter.Converter

internal class TangemPayCashbackUmConverter(
    private val dateFormatter: TangemPayCashbackDateFormatter = TangemPayCashbackDateFormatter(),
) : Converter<TangemPayCashback?, TangemPayCashbackUM> {

    // TODO([REDACTED_TASK_KEY]): move hardcoded strings to string resources
    override fun convert(value: TangemPayCashback?): TangemPayCashbackUM {
        if (value == null || value.confirmedAmount.signum() == 0) {
            return TangemPayCashbackUM(
                title = stringReference("Start spending and earn cashback"),
                subtitle = stringReference("Collected amount will be shown here"),
                isEmpty = true,
                banner = null,
            )
        }
        val currency = getJavaCurrencyByCode(value.currency)
        val earned = value.confirmedAmount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
        val month = dateFormatter.formatMonth(value.period.year, value.period.month)
        val payoutWindow = dateFormatter.formatWindow(value.period.payoutStart, value.period.payoutEnd)
        val payoutEnd = dateFormatter.formatMonthDay(value.period.payoutEnd)
        val banner = if (value.confirmedAmount.signum() < 0) {
            TangemPayCashbackUM.Banner(
                text = stringReference(
                    "We received a refund for a purchase for which cashback had previously been awarded",
                ),
                type = TangemPayCashbackUM.Banner.Type.Error,
            )
        } else {
            TangemPayCashbackUM.Banner(
                text = stringReference("Cashback $earned for $month will be deposited till $payoutEnd"),
                type = TangemPayCashbackUM.Banner.Type.Info,
            )
        }
        return TangemPayCashbackUM(
            title = stringReference("$earned earned in $month"),
            subtitle = stringReference("Will be deposited on $payoutWindow"),
            isEmpty = false,
            banner = banner,
        )
    }
}