package com.tangem.tap.features.wallet.ui.analytics

import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

class WalletAnalyticsEventsMapper : Converter<TotalFiatBalance, AnalyticsParam.CardBalanceState?> {

    override fun convert(value: TotalFiatBalance): AnalyticsParam.CardBalanceState? {
        return when (value) {
            is TotalFiatBalance.Failed -> AnalyticsParam.CardBalanceState.BlockchainError
            is TotalFiatBalance.Loaded -> when {
                value.isWarning -> AnalyticsParam.CardBalanceState.CustomToken
                value.amount.isGreaterThan(BigDecimal.ZERO) -> AnalyticsParam.CardBalanceState.Full
                else -> AnalyticsParam.CardBalanceState.Empty
            }
            is TotalFiatBalance.Loading -> null
        }
    }
}
