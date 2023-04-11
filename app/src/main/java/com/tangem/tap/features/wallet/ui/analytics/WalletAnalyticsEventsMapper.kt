package com.tangem.tap.features.wallet.ui.analytics

import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

class WalletAnalyticsEventsMapper : Converter<TotalFiatBalance, AnalyticsParam.CardBalanceState?> {

    override fun convert(value: TotalFiatBalance): AnalyticsParam.CardBalanceState? {
        return when (value) {
            is TotalFiatBalance.Error -> {
                if (value.amount == null) {
                    // if fiatAmount is null while ProgressState.Error it means error occurs when loading blockchain
                    AnalyticsParam.CardBalanceState.BlockchainError
                } else {
                    // if fiatAmount is not null while ProgressState.Error it means couldn't load custom token amount
                    AnalyticsParam.CardBalanceState.CustomToken
                }
            }
            is TotalFiatBalance.Loaded -> {
                if (value.amount.isGreaterThan(BigDecimal.ZERO)) {
                    AnalyticsParam.CardBalanceState.Full
                } else {
                    AnalyticsParam.CardBalanceState.Empty
                }
            }
            is TotalFiatBalance.Loading -> null
        }
    }
}