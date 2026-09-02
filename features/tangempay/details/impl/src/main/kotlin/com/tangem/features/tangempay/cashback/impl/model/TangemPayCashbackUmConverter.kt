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
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.isZero
import org.joda.time.LocalDate
import java.util.Currency

internal class TangemPayCashbackUmConverter(
    private val dateFormatter: TangemPayCashbackDateFormatter = TangemPayCashbackDateFormatter(),
    private val today: () -> LocalDate = { LocalDate.now() },
) : Converter<TangemPayCashback?, TangemPayCashbackUM> {

    override fun convert(value: TangemPayCashback?): TangemPayCashbackUM {
        if (value == null) return emptyState(banner = null)
        val currency = getJavaCurrencyByCode(value.currency)
        val depositBanner = value.previousPayout
            ?.takeIf(::isAwaitingDeposit)
            ?.let { depositBanner(payout = it, currency = currency) }
        return if (value.confirmedAmount.isZero()) {
            emptyState(banner = depositBanner)
        } else {
            earnedState(value = value, currency = currency, depositBanner = depositBanner)
        }
    }

    private fun emptyState(banner: TangemPayCashbackUM.Banner?): TangemPayCashbackUM = TangemPayCashbackUM(
        title = resourceReference(R.string.tangempay_cashback_empty_title),
        subtitle = resourceReference(R.string.tangempay_cashback_empty_subtitle),
        isEmpty = true,
        banner = banner,
    )

    private fun earnedState(
        value: TangemPayCashback,
        currency: Currency,
        depositBanner: TangemPayCashbackUM.Banner?,
    ): TangemPayCashbackUM {
        val earned = value.confirmedAmount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
        val monthIn = arrayItemReference(R.array.common_month_in, value.period.month - 1)
        val isNegative = value.confirmedAmount.isNegative()
        val banner = if (isNegative) {
            TangemPayCashbackUM.Banner(
                text = resourceReference(R.string.tangempay_cashback_refund_banner),
                type = TangemPayCashbackUM.Banner.Type.Error,
            )
        } else {
            depositBanner
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

    // The banner promises a deposit "till <endDate>", so it stays through that whole day and goes the day after
    private fun isAwaitingDeposit(payout: TangemPayCashback.PreviousPayout): Boolean =
        payout.amount.isPositive() && !payout.endDate.toLocalDate().isBefore(today())

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