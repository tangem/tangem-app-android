package com.tangem.tap.common.analytics.converters

import com.tangem.common.Converter
import com.tangem.common.extensions.isZero
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.ScanResponse
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.filters.BasicTopUpFilter
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.features.wallet.redux.WalletData
import com.tangem.tap.features.wallet.redux.WalletState
import com.tangem.tap.features.wallet.redux.reducers.calculateTotalCryptoAmount

/**
* [REDACTED_AUTHOR]
 */
class BasicSignInEventConverter(
    private val scanResponse: ScanResponse,
) : Converter<WalletState, Basic.SignedIn?> {

    override fun convert(value: WalletState): Basic.SignedIn? {
        if (!statesIsReadyToCreateEvent(scanResponse.cardTypesResolver, value)) return null
        val cardCurrency = ParamCardCurrencyConverter().convert(scanResponse.cardTypesResolver) ?: return null

        return Basic.SignedIn(
            state = AnalyticsParam.CardBalanceState.from(value.walletsDataFromStores),
            currency = cardCurrency,
            batch = scanResponse.card.batchId,
        ).apply {
            filterData = UserWalletIdBuilder.scanResponse(scanResponse)
                .build()
                ?.stringValue
        }
    }
}

class BasicTopUpEventConverter(
    private val scanResponse: ScanResponse,
) : Converter<WalletState, Basic.ToppedUp?> {

    override fun convert(value: WalletState): Basic.ToppedUp? {
        if (!statesIsReadyToCreateEvent(scanResponse.cardTypesResolver, value)) return null
        val cardCurrency = ParamCardCurrencyConverter().convert(scanResponse.cardTypesResolver) ?: return null

        val data = BasicTopUpFilter.Data(
            walletId = UserWalletIdBuilder.scanResponse(scanResponse).build()?.stringValue ?: "",
            cardBalanceState = AnalyticsParam.CardBalanceState.from(value.walletsDataFromStores),
        )

        return Basic.ToppedUp(cardCurrency).apply { filterData = data }
    }
}

private fun AnalyticsParam.CardBalanceState.Companion.from(
    walletsData: List<WalletData>,
): AnalyticsParam.CardBalanceState {
    val totalCryptoAmount = walletsData.calculateTotalCryptoAmount()
    return when {
        totalCryptoAmount.isZero() -> AnalyticsParam.CardBalanceState.Empty
        else -> AnalyticsParam.CardBalanceState.Full
    }
}

private fun statesIsReadyToCreateEvent(cardTypesResolver: CardTypesResolver, state: WalletState): Boolean {
    if (cardTypesResolver.isMultiwalletAllowed() && state.missingDerivations.isNotEmpty()) return false
    if (state.walletsDataFromStores.isEmpty()) return false

    val totalBalanceState = state.totalBalance?.state ?: return false
    if (totalBalanceState == ProgressState.Loading || totalBalanceState == ProgressState.Refreshing) return false

    val balancesCount = state.walletsDataFromStores.map {
        if (it.currencyData.amount == null) 0 else 1
    }.reduce { acc, i -> acc + i }

    if (balancesCount != state.walletsStores.size) return false

    return true
}
