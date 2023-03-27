package com.tangem.tap.common.analytics.converters

import com.tangem.common.Converter
import com.tangem.common.extensions.isZero
import com.tangem.domain.common.CardTypesResolver
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.filters.TopUpFilter
import com.tangem.tap.domain.model.WalletDataModel
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
class TopUpEventConverter : Converter<TopUpEventConverter.Data, Basic.ToppedUp?> {

    override fun convert(value: Data): Basic.ToppedUp? {
        val (cardTypesResolver, walletDataModels, userWalletIdValue, isToppedUpInPast) = value
        val paramCardCurrency = ParamCardCurrencyConverter().convert(cardTypesResolver) ?: return null

        val cardBalanceState = BalanceCalculator(walletDataModels).calculate().toCardBalanceState()
        val filterData = TopUpFilter.Data(
            walletId = userWalletIdValue,
            cardBalanceState = cardBalanceState,
            isToppedUpInPast = isToppedUpInPast,
        )
        return Basic.ToppedUp(paramCardCurrency).apply {
            this.filterData = filterData
        }
    }

    private fun BigDecimal.toCardBalanceState(): AnalyticsParam.CardBalanceState = when {
        isZero() -> AnalyticsParam.CardBalanceState.Empty
        else -> AnalyticsParam.CardBalanceState.Full
    }

    data class Data(
        val cardTypesResolver: CardTypesResolver,
        val walletDataModels: List<WalletDataModel>,
        val userWalletIdValue: String,
        val isToppedUpInPast: Boolean,
    )
}

private interface IBalanceCalculator {
    fun calculate(): BigDecimal
}

private class BalanceCalculator(
    private val walletDataModels: List<WalletDataModel>,
) : IBalanceCalculator {

    override fun calculate(): BigDecimal {
        val singleToken = walletDataModels
            .filter { it.currency.isToken() }
            .firstOrNull { it.isCardSingleToken }

        val totalAmount = singleToken?.status?.amount
            ?: walletDataModels.calculateTotalCryptoAmount()

        return totalAmount
    }

    private fun List<WalletDataModel>.calculateTotalCryptoAmount(): BigDecimal = this
        .map { it.status.amount }
        .reduce(BigDecimal::plus)
}
