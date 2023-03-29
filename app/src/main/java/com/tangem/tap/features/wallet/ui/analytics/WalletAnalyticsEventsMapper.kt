package com.tangem.tap.features.wallet.ui.analytics

import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.features.wallet.models.TotalBalance
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

class WalletAnalyticsEventsMapper : Converter<TotalBalance, AnalyticsParam.CardBalanceState?> {

    override fun convert(value: TotalBalance): AnalyticsParam.CardBalanceState? {
        return when (value.state) {
            ProgressState.Done -> if (value.fiatAmount?.isGreaterThan(BigDecimal.ZERO) == true) {
                AnalyticsParam.CardBalanceState.Full
            } else {
                AnalyticsParam.CardBalanceState.Empty
            }
            ProgressState.Error -> {
                if (value.fiatAmount == null) {
                    // if fiatAmount is null while ProgressState.Error it means error occurs when loading blockchain
                    AnalyticsParam.CardBalanceState.BlockchainError
                } else {
                    // if fiatAmount is not null while ProgressState.Error it means couldn't load custom token amount
                    AnalyticsParam.CardBalanceState.CustomToken
                }
            }
            else -> null
        }
    }
}