package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.arrayItemReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
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

    override fun convert(value: TangemPayCashback?): TangemPayCashbackUM {
        if (value == null || value.confirmedAmount.signum() == 0) {
            return TangemPayCashbackUM(
                title = resourceReference(R.string.tangempay_cashback_empty_title),
                subtitle = resourceReference(R.string.tangempay_cashback_empty_subtitle),
                isEmpty = true,
                banner = null,
            )
        }
        val currency = getJavaCurrencyByCode(value.currency)
        val earned = value.confirmedAmount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
        val month = dateFormatter.formatMonth(value.period.year, value.period.month)
        val monthIn = arrayItemReference(R.array.common_month_in, value.period.month - 1)
        val payoutWindow = dateFormatter.formatWindow(value.period.payoutStart, value.period.payoutEnd)
        val payoutEnd = dateFormatter.formatMonthDay(value.period.payoutEnd)
        val banner = if (value.confirmedAmount.signum() < 0) {
            TangemPayCashbackUM.Banner(
                text = resourceReference(R.string.tangempay_cashback_refund_banner),
                type = TangemPayCashbackUM.Banner.Type.Error,
            )
        } else {
            TangemPayCashbackUM.Banner(
                text = resourceReference(
                    id = R.string.tangempay_cashback_deposit_banner,
                    formatArgs = wrappedList(earned, month, payoutEnd),
                ),
                type = TangemPayCashbackUM.Banner.Type.Info,
            )
        }
        return TangemPayCashbackUM(
            title = resourceReference(R.string.tangempay_cashback_earned_title, wrappedList(earned, monthIn)),
            subtitle = resourceReference(R.string.tangempay_cashback_deposited_on, wrappedList(payoutWindow)),
            isEmpty = false,
            banner = banner,
        )
    }
}