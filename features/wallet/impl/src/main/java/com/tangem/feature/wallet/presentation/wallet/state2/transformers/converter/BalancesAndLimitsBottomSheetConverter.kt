package com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter

import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state2.model.BalancesAndLimitsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class BalancesAndLimitsBottomSheetConverter(
    private val eventSender: WalletEventSender,
) : Converter<VisaCurrency, BalancesAndLimitsBottomSheetConfig> {

    override fun convert(value: VisaCurrency): BalancesAndLimitsBottomSheetConfig {
        return BalancesAndLimitsBottomSheetConfig(
            currency = value.symbol,
            balance = BalancesAndLimitsBottomSheetConfig.Balance(
                totalBalance = value.balances.total.let(::formatAmount),
                availableBalance = value.balances.available.let(::formatAmount),
                blockedBalance = value.balances.blocked.let(::formatAmount),
                debit = value.balances.debt.let(::formatAmount),
                pending = value.balances.pendingRefund.let(::formatAmount),
                amlVerified = value.balances.verified.let(::formatAmount),
            ),
            limit = BalancesAndLimitsBottomSheetConfig.Limit(
                availableBy = DateTimeFormatters.formatDate(date = value.limits.expirationDate),
                inStore = value.limits.remainingOtp.let(::formatAmount),
                other = value.limits.remainingNoOtp.let(::formatAmount),
                singleTransaction = value.limits.singleTransaction.let(::formatAmount),
            ),
            onBalanceInfoClick = this::showBalanceInfo,
            onLimitInfoClick = this::showLimitInfo,
        )
    }

    private fun formatAmount(amount: BigDecimal): String = BigDecimalFormatter.formatCryptoAmount(
        amount,
        cryptoCurrency = "",
        decimals = 2,
    )

    private fun showBalanceInfo() {
        eventSender.send(WalletEvent.ShowAlert(WalletAlertState.VisaBalancesInfo))
    }

    private fun showLimitInfo() {
        eventSender.send(WalletEvent.ShowAlert(WalletAlertState.VisaLimitsInfo))
    }
}