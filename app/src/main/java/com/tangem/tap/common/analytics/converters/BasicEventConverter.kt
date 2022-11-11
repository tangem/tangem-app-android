package com.tangem.tap.common.analytics.converters

import com.tangem.common.Converter
import com.tangem.common.extensions.isZero
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.userWalletId
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.filters.BasicTopUpFilter
import com.tangem.tap.domain.extensions.isMultiwalletAllowed
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.reducers.calculateTotalCryptoAmount

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class BasicSignInEventConverter(
    private val scanResponse: ScanResponse,
) : Converter<WalletState, Basic.SignedIn?> {

    override fun convert(value: WalletState): Basic.SignedIn? {
        if (!statesIsReadyToCreateEvent(scanResponse, value)) return null
        val cardCurrency = ParamCardCurrencyConverter().convert(scanResponse) ?: return null

        return Basic.SignedIn(
            state = AnalyticsParam.CardBalanceState.from(value.walletsData),
            currency = cardCurrency,
            batch = scanResponse.card.batchId,
        ).apply {
            filterData = scanResponse.card.userWalletId.stringValue
        }
    }
}

class BasicTopUpEventConverter(
    private val scanResponse: ScanResponse,
) : Converter<WalletState, Basic.ToppedUp?> {

    override fun convert(value: WalletState): Basic.ToppedUp? {
        if (!statesIsReadyToCreateEvent(scanResponse, value)) return null
        val cardCurrency = ParamCardCurrencyConverter().convert(scanResponse) ?: return null

        val data = BasicTopUpFilter.Data(
            walletId = scanResponse.card.userWalletId.stringValue,
            cardBalanceState = AnalyticsParam.CardBalanceState.from(value.walletsData),
        )

        return Basic.ToppedUp(cardCurrency).apply { filterData = data }
    }
}

private fun AnalyticsParam.CardBalanceState.Companion.from(walletsData: List<WalletData>): AnalyticsParam.CardBalanceState {
    val totalCryptoAmount = walletsData.calculateTotalCryptoAmount()
    return when {
        totalCryptoAmount.isZero() -> AnalyticsParam.CardBalanceState.Empty
        else -> AnalyticsParam.CardBalanceState.Full
    }
}

private fun statesIsReadyToCreateEvent(scanResponse: ScanResponse, state: WalletState): Boolean {
    if (scanResponse.card.isMultiwalletAllowed && state.missingDerivations.isNotEmpty()) return false
    if (state.walletsData.isEmpty()) return false

    val totalBalanceState = state.totalBalance?.state ?: return false
    if (totalBalanceState == ProgressState.Loading || totalBalanceState == ProgressState.Refreshing) return false

    val balancesCount = state.walletsData.map {
        if (it.currencyData.amount == null) 0 else 1
    }.reduce { acc, i -> acc + i }

    if (balancesCount != state.wallets.size) return false

    return true
}
