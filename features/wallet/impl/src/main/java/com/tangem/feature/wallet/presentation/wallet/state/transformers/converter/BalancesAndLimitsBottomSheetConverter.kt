package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.MainScreenAnalyticsEvent
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.feature.wallet.presentation.wallet.state.model.BalancesAndLimitsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletEventSender
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class BalancesAndLimitsBottomSheetConverter(
    private val eventSender: WalletEventSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Converter<VisaCurrency, BalancesAndLimitsBottomSheetConfig> {

    override fun convert(value: VisaCurrency): BalancesAndLimitsBottomSheetConfig {
        fun formatAmount(amount: BigDecimal): String = amount.format { crypto(value.symbol, value.decimals) }

        val otpLimit = value.limits.remainingOtp.let(::formatAmount)
        val noOtpLimit = value.limits.remainingNoOtp.let(::formatAmount)

        return BalancesAndLimitsBottomSheetConfig(
            balance = BalancesAndLimitsBottomSheetConfig.Balance(
                totalBalance = value.balances.total.let(::formatAmount),
                availableBalance = value.balances.available.let(::formatAmount),
                blockedBalance = value.balances.blocked.let(::formatAmount),
                debit = value.balances.debt.let(::formatAmount),
                amlVerified = value.balances.verified.let(::formatAmount),
                onInfoClick = this::balanceInfoOnClick,
            ),
            limit = BalancesAndLimitsBottomSheetConfig.Limit(
                availableBy = DateTimeFormatters.formatDate(date = value.limits.expirationDate),
                total = otpLimit,
                other = noOtpLimit,
                singleTransaction = value.limits.singleTransaction.let(::formatAmount),
                onInfoClick = { limitInfoOnClick(otpLimit, noOtpLimit) },
            ),
        )
    }

    private fun balanceInfoOnClick() {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.NoticeBalancesInfo)
        eventSender.send(WalletEvent.ShowAlert(WalletAlertState.VisaBalancesInfo))
    }

    private fun limitInfoOnClick(totalLimit: String, otherLimit: String) {
        analyticsEventHandler.send(MainScreenAnalyticsEvent.NoticeLimitsInfo)
        eventSender.send(WalletEvent.ShowAlert(WalletAlertState.VisaLimitsInfo(totalLimit, otherLimit)))
    }
}