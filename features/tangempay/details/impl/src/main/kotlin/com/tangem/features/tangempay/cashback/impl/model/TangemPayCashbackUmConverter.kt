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
import com.tangem.utils.extensions.isNegative
import com.tangem.utils.extensions.isZero
import java.util.Currency

internal class TangemPayCashbackUmConverter(
    private val dateFormatter: TangemPayCashbackDateFormatter = TangemPayCashbackDateFormatter(),
) : Converter<TangemPayCashback?, TangemPayCashbackUM> {

    override fun convert(value: TangemPayCashback?): TangemPayCashbackUM {
        if (value == null || value.confirmedAmount.isZero()) {
            return TangemPayCashbackUM(
                title = resourceReference(R.string.tangempay_cashback_empty_title),
                subtitle = resourceReference(R.string.tangempay_cashback_empty_subtitle),
                isEmpty = true,
                banner = null,
            )
        }
        val currency = getJavaCurrencyByCode(value.currency)
        val earned = value.confirmedAmount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
        val monthIn = arrayItemReference(R.array.common_month_in, value.period.month - 1)
        val isNegative = value.confirmedAmount.isNegative()
        val previousPayout = value.previousPayout
        val banner = when {
            isNegative -> TangemPayCashbackUM.Banner(
                text = resourceReference(R.string.tangempay_cashback_refund_banner),
                type = TangemPayCashbackUM.Banner.Type.Error,
            )
            previousPayout == null -> null
            else -> depositBanner(payout = previousPayout, currency = currency)
        }
        val subtitle = if (isNegative) {
            null
        } else {
            dateFormatter.formatWindow(value.period.payoutStart, value.period.payoutEnd)
                ?.let { resourceReference(R.string.tangempay_cashback_deposited_on, wrappedList(it)) }
        }
        return TangemPayCashbackUM(
            title = resourceReference(R.string.tangempay_cashback_earned_title, wrappedList(earned, monthIn)),
            subtitle = subtitle,
            isEmpty = false,
            banner = banner,
        )
    }

    private fun depositBanner(
        payout: TangemPayCashback.PreviousPayout,
        currency: Currency,
    ): TangemPayCashbackUM.Banner {
        val amount = payout.amount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
        // The payout always lands in the month after the one it was earned in
        val earnedMonth = payout.endDate.minusMonths(1)
        return TangemPayCashbackUM.Banner(
            text = resourceReference(
                id = R.string.tangempay_cashback_deposit_banner,
                formatArgs = wrappedList(
                    amount,
                    dateFormatter.formatMonth(earnedMonth.year, earnedMonth.monthOfYear),
                    dateFormatter.formatMonthDay(payout.endDate),
                ),
            ),
            type = TangemPayCashbackUM.Banner.Type.Info,
        )
    }
}